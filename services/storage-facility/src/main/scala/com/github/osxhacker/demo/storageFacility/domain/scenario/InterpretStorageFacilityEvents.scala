package com.github.osxhacker.demo.storageFacility.domain.scenario

import cats.data.Kleisli
import eu.timepit.refined
import org.typelevel.log4cats
import org.typelevel.log4cats.{
	LoggerFactory,
	StructuredLogger
	}

import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.chassis.domain.error.{
	ObjectNotFoundError,
	StaleObjectError
	}

import com.github.osxhacker.demo.chassis.effect.Pointcut
import com.github.osxhacker.demo.chassis.monitoring.Subsystem
import com.github.osxhacker.demo.chassis.domain.repository.UpdateIntent
import com.github.osxhacker.demo.storageFacility.domain._
import com.github.osxhacker.demo.storageFacility.domain.event._


/**
 * The '''InterpretStorageFacilityEvents''' type defines the Domain Object Model
 * Use-Case scenario responsible for interpreting
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]]-related
 * [[com.github.osxhacker.demo.storageFacility.domain.event]]s.
 *
 * Since [[com.github.osxhacker.demo.storageFacility.domain.event]]s handled
 * here are from installations __other__ than what this service is responsible
 * for (by definition), no events are emitted here.
 */
final case class InterpretStorageFacilityEvents[F[_]] ()
	(
		implicit

		/// Needed for ''scenarios''.
		override protected val compiler : fs2.Compiler.Target[F],

		/// Needed for logging.
		override protected val loggerFactory : LoggerFactory[F],

		/// Needed for ''scenarios''.
		override protected val pointcut : Pointcut[F],

		/// Needed for `scopeWith`.
		override protected val subsystem : Subsystem
	)
	extends AbstractEventInterpreter[F, StorageFacility, AllStorageFacilityEvents] ()
{
	/// Class Imports
	import cats.syntax.all._
	import log4cats.syntax._


	/// Class Types
	private object dispatch
		extends EventDispatcher
	{
		/// Class Imports
		import refined.cats._


		/// Implicit Conversions
		implicit def caseCreated (
			implicit
			env : ScopedEnvironment[F],
			logger : StructuredLogger[F]
			)
			: Case.Aux[StorageFacilityCreated, F[Unit]] =
			handler[StorageFacilityCreated] ("create facility: " + _.id.show) {
				event =>
					for {
						_ <- debug"resolving tenant: id=${event.owner.show}"
						tenant <- scenarios.findCompany (event.owner)

						_ <- debug"creating facility: name='${event.name.show}''"
						result <- scenarios.create (event -> tenant)

						unit <- debug"created facility: ${result.id.show}"
						} yield unit
				}


		implicit def caseDeleted (
			implicit
			env : ScopedEnvironment[F],
			logger : StructuredLogger[F]
			)
			: Case.Aux[StorageFacilityDeleted, F[Unit]] =
			handler[StorageFacilityDeleted] ("delete facility: " + _.id.show) {
				event =>
					for {
						_ <- debug"resolving facility: id=${event.id.show}"
						facility <- scenarios.findFacility (event.id)

						_ <- debug"deleting facility: id='${facility.id.show}''"
						result <- scenarios.delete (facility)

						unit <- debug"deleted facility? $result"
					} yield unit
			}


		implicit def caseProfileChanged (
			implicit
			env : ScopedEnvironment[F],
			logger : StructuredLogger[F]
			)
			: Case.Aux[StorageFacilityProfileChanged, F[Unit]] =
			handler[StorageFacilityProfileChanged] ("facility profile changed: " + _.id.show) {
				event =>
					for {
						_ <- debug"resolving facility: id=${event.id.show}"
						existing <- scenarios.findFacility (event.id)

						_ <- debug"modifying profile: id=${existing.id.show}"
						updated <- scenarios.save (
							UpdateIntent (
								StorageFacility.name
									.replace (event.name)
									.andThen (
										StorageFacility.city
											.replace (event.city)
										)
									.andThen (
										StorageFacility.state
											.replace (event.state)
										)
									.andThen (
										StorageFacility.zip
											.replace (event.zip)
										) (existing)
								)
							)

						unit <- debug"profile altered: ${updated.map (_.id.show)}"
						} yield unit
				}


		implicit def caseStatusChanged (
			implicit
			env : ScopedEnvironment[F],
			logger : StructuredLogger[F]
			)
			: Case.Aux[StorageFacilityStatusChanged, F[Unit]] =
			handler[StorageFacilityStatusChanged] ("facility status changed: " + _.id.show) {
				event =>
					for {
						_ <- debug"resolving facility: id=${event.id.show}"
						facility <- scenarios.findFacility (event.id)

						_ <- debug"changing facility status: id='${facility.id.show}''"
						result <- scenarios.changeStatus (
							facility,
							facility.version,
							event.status
							)

						unit <- debug"altered facility: $result"
					} yield unit
			}
	}


	private object scenarios
	{
		/// Instance Properties
		val changeStatus = ChangeFacilityStatus[F] ()
		lazy val create = CreateFacility[F] (
			capacity = createdEventOnly.map (_.capacity),
			available = createdEventOnly.map (_.available)
			) {
			Kleisli[ErrorOr, (StorageFacilityCreated, Company), StorageFacility] {
				case (event, owner) =>
					event.toStorageFacility[ErrorOr] (owner)
				}
			}

		val delete = DeleteFacility[F] ().enableMultiRegion ()
		val findCompany = FindCompany[F] ()
		val findFacility = FindFacility[F] ()
		val save = SaveFacility[F] ()

		private val createdEventOnly = Kleisli[
			ErrorOr,
			(StorageFacilityCreated, Company),
			StorageFacilityCreated
			] (_._1.pure[ErrorOr])
	}


	private object scopeEnvironment
		extends MakeScopedEnvironment ()
	{
		/// Implicit Conversions
		implicit def caseEvent[EventT <: StorageFacilityEvent] (
			implicit global : GlobalEnvironment[F]
			)
			: Case.Aux[EventT, F[ScopedEnvironment[F]]] =
			definition[StorageFacilityEvent, EventT] (
				StorageFacilityEvent.id,
				StorageFacilityEvent.correlationId,
				StorageFacilityEvent.owner,
				StorageFacilityEvent.region
				)
	}


	def apply ()
		: Kleisli[F, (AllStorageFacilityEvents, GlobalEnvironment[F]), Unit] =
		Kleisli {
			case (event, original) =>
				implicit val global = original

				event.map (scopeEnvironment)
					.unify
					.fproduct (_.loggingFactory.getLogger)
					.flatMap {
						case (env, logger) =>
							interpret (event) (env, logger)
					}
		}


	private def interpret (event : AllStorageFacilityEvents)
		(
			implicit
			env : ScopedEnvironment[F],
			logger : StructuredLogger[F]
		)
		: F[Unit] =
		event.map (dispatch)
			.unify
			.handleErrorWith (reportOrPropagate)


	private def reportOrPropagate (problem : Throwable)
		(implicit logger : StructuredLogger[F])
		: F[Unit] =
		problem match {
			case e : ObjectNotFoundError[_] =>
				warn"unable to interpret company event: ${e.getMessage}"

			case e : StaleObjectError[_] =>
				warn"unable to interpret company event: ${e.getMessage}"

			case other =>
				error"unrecoverable error when interpreting company event" >>
				other.raiseError[F, Unit]
		}
}

