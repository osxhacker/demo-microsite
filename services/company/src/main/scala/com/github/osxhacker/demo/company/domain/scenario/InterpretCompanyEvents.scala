package com.github.osxhacker.demo.company.domain.scenario

import cats.MonadThrow
import cats.data.Kleisli
import eu.timepit.refined
import monocle.{
	Getter,
	Lens
	}

import monocle.macros.Lenses
import org.typelevel.log4cats
import org.typelevel.log4cats.{
	LoggerFactory,
	StructuredLogger
	}

import shapeless.{
	syntax => _,
	Lens => _,
	_
	}

import com.github.osxhacker.demo.chassis
import com.github.osxhacker.demo.chassis.domain.{
	ErrorOr,
	Slug
	}

import com.github.osxhacker.demo.chassis.domain.entity.Version
import com.github.osxhacker.demo.chassis.domain.event.SuppressEvents
import com.github.osxhacker.demo.chassis.domain.error.{
	LogicError,
	ObjectNotFoundError,
	StaleObjectError
	}

import com.github.osxhacker.demo.chassis.domain.repository.UpdateIntent
import com.github.osxhacker.demo.chassis.effect.Pointcut
import com.github.osxhacker.demo.chassis.monitoring.Subsystem
import com.github.osxhacker.demo.chassis.monitoring.metrics.UseCaseScenario
import com.github.osxhacker.demo.company.domain.{
	GlobalEnvironment,
	ScopedEnvironment
	}

import com.github.osxhacker.demo.company.domain.Company
import com.github.osxhacker.demo.company.domain.event._


/**
 * The '''InterpretCompanyEvents''' type defines the Domain Object Model
 * Use-Case scenario responsible for interpreting
 * [[com.github.osxhacker.demo.company.domain.Company]]-related
 * [[com.github.osxhacker.demo.company.domain.event]]s.
 */
final case class InterpretCompanyEvents[F[_]] ()
	(
		implicit

		/// Needed for ''scenarios''.
		private val compiler : fs2.Compiler.Target[F],

		/// Needed for logging.
		private val loggerFactory : LoggerFactory[F],

		/// Needed for ''scenarios''.
		private val pointcut : Pointcut[F],

		/// Needed for `scopeWith`.
		private val subsystem : Subsystem
	)
	extends SuppressEvents[ScopedEnvironment[F], AllCompanyEvents]
{
	/// Class Imports
	import InterpretCompanyEvents.{
		ChangeProperty,
		ProfileSource
		}

	import cats.syntax.all._
	import log4cats.syntax._


	/// Class Types
	private object dispatch
		extends Poly1
	{
		/// Class Imports
		import refined.cats._


		@inline
		private def handler[EventT] (description : EventT => String)
			(block : EventT => F[Unit])
			(implicit logger : StructuredLogger[F])
			: Case.Aux[EventT, F[Unit]] =
			at {
				event =>
					block (event).recoverWith {
						case problem =>
							logger.warn (problem) (
								s"unable to ${description (event)}"
								)
						}
				}


		/// Implicit Conversions
		implicit def caseCreated (
			implicit
			env : ScopedEnvironment[F],
			logger : StructuredLogger[F]
			)
			: Case.Aux[CompanyCreated, F[Unit]] =
			handler[CompanyCreated] ("create company: " + _.slug.show) {
				event =>
					for {
						_ <- debug"creating company: slug=${event.slug.show}"
						company <- scenarios.create (event)

						unit <- debug"new company: ${company.id.show} (${company.slug.show})"
						} yield unit
				}


		implicit def caseDeleted (
			implicit
			env : ScopedEnvironment[F],
			logger : StructuredLogger[F]
			)
			: Case.Aux[CompanyDeleted, F[Unit]] =
			handler[CompanyDeleted] ("delete company: " + _.id.show) {
				event =>
					for {
						_ <- debug"resolving company: id=${event.id.show}"
						company <- scenarios.findById (event.id)

						_ <- debug"found company: id=${company.id.show}"
						deleted <- scenarios.delete (company)

						unit <- debug"removed company? $deleted"
						} yield unit
				}


		implicit def caseProfileChanged (
			implicit
			env : ScopedEnvironment[F],
			logger : StructuredLogger[F]
			)
			: Case.Aux[CompanyProfileChanged, F[Unit]] =
			handler[CompanyProfileChanged] ("change profile: " + _.name.show) {
				event =>
					for {
						_ <- debug"resolving company: id=${event.id.show}"
						existing <- scenarios.findById (event.id)

						_ <- debug"found company: id=${existing.id.show}"
						saved <- scenarios.changeProfile (
							existing,
							ProfileSource (event, existing)
							)

						unit <- debug"changed company profile: id=${saved.id.show}"
						} yield unit
				}


		implicit def caseSlugChanged (
			implicit
			env : ScopedEnvironment[F],
			logger : StructuredLogger[F]
			)
			: Case.Aux[CompanySlugChanged, F[Unit]] =
			handler[CompanySlugChanged] ("change slug: " + _.from.show) {
				scenarios.changeSlug (_)
				}


		implicit def caseStatusChanged (
			implicit
			env : ScopedEnvironment[F],
			logger : StructuredLogger[F]
			)
			: Case.Aux[CompanyStatusChanged, F[Unit]] =
			handler[CompanyStatusChanged] ("change status: " + _.status.show) {
				event =>
					for {
						_ <- debug"resolving company: id=${event.id.show}"
						company <- scenarios.findById (event.id)

						_ <- debug"found company: id=${company.id.show}"
						saved <- scenarios.changeStatus (
							company,
							company.version,
							event.status
							)

						unit <- debug"changed company status: id=${saved.id.show}"
						} yield unit
				}
	}


	private object scenarios
	{
		val changeSlug = ChangeProperty[F, CompanySlugChanged, Slug] (
			CompanySlugChanged.to,
			Company.slug
			)

		val changeProfile = ChangeCompany[F] (
			id = ProfileSource.event
				.andThen (CompanyProfileChanged.id),

			version = ProfileSource.company
				.andThen (Company.version)
				.andThen (Version.value),

			slug = ProfileSource.company
				.andThen (Company.slug)
				.andThen (Slug.value),

			status = ProfileSource.company
				.andThen (Company.status)
			) (Kleisli[ErrorOr, ProfileSource, Company] (_ ().pure[ErrorOr]))

		val changeStatus = ChangeCompanyStatus[F] ()
		val create = CreateCompany[F] (
			CompanyCreated.slug
				.andThen (Slug.value)
			) (
			Kleisli[ErrorOr, CompanyCreated, Company] (
				_.toCompany ()
					.pure[ErrorOr]
				)
			)

		val delete = DeleteCompany[F] ()
		val findById = FindCompany[F] ()
	}


	private object scopeEnvironment
		extends Poly1
	{
		/// Implicit Conversions
		implicit def caseEvent[EventT <: CompanyEvent] (
			implicit global : GlobalEnvironment[F]
			)
			: Case.Aux[EventT, F[ScopedEnvironment[F]]] =
			at {
				event =>
					global.scopeWith (event.correlationId)
						.map {
							_.addContext (
								Map (
									"eventId" -> event.id.show,
									"eventType" -> event.getClass.getName,
									"region" -> event.region.show
									) ++
									event.fingerprint
										.map {
											value =>
												Map (
													"fingerprint" -> value.show
													)
											}
										.orEmpty
								)
							}
				}
	}


	def apply () : Kleisli[F, (AllCompanyEvents, GlobalEnvironment[F]), Unit] =
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


	private def interpret (event : AllCompanyEvents)
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


object InterpretCompanyEvents
{
	/// Class Types
	/**
	 * The '''ChangeProperty''' type defines an
	 * ''InterpretCompanyEvents''-specific Use-Case scenario which is
	 * responsible for altering a single
	 * [[com.github.osxhacker.demo.company.domain.Company]] property within the
	 * [[com.github.osxhacker.demo.company.domain.repository.CompanyRepository]].
	 *
	 * Should this Use-Case be applicable for use in other scenarios, it can be
	 * refactored at that time.
	 */
	final private case class ChangeProperty[
		F[_],
		EventT <: CompanyEvent,
		ValueT
		] (
			private val value : Getter[EventT, ValueT],
			private val lens : Lens[Company, ValueT]
		)
		(
			implicit

			/// Needed for `save`.
			monadThrow : MonadThrow[F],

			/// Needed for scenarios.
			private val pointcut : Pointcut[F],
		)
	{
		/// Class Imports
		import cats.syntax.all._
		import chassis.syntax._
		import log4cats.syntax._


		/// Instance Properties
		private val find = FindCompany[F] ()
		private val save = SaveCompany[F] ()


		def apply (event : EventT)
			(
				implicit
				env : ScopedEnvironment[F],
				logger : StructuredLogger[F],
			)
			: F[Unit] =
			change (event).measure[
				UseCaseScenario[
					F,
					ChangeProperty[F, EventT, ValueT],
					Unit
					]
				] ()


		private def change (event : EventT)
			(
				implicit
				env : ScopedEnvironment[F],
				logger : StructuredLogger[F],
			)
			: F[Unit] =
			for {
				_ <- debug"resolving company: id=${event.id.show}"
				existing <- find (event.id)

				_ <- debug"found company: id=${existing.id.show}"

				result <- save (
					UpdateIntent (
						lens.replace (value.get (event)) (existing)
						)
					)

				latest <- result.toRight (
					LogicError ("save did not produce an updated Company")
					)
					.liftTo[F]

				id = latest.id.show
				from = lens.get (existing)
				to = value.get (event)

				unit <- debug"changed company: id=$id '$from' '$to'"
				} yield unit
	}


	/**
	 * The '''ProfileSource''' type is an implementation abstraction used in
	 * conjunction with
	 * [[com.github.osxhacker.demo.company.domain.scenario.ChangeCompany]].  It
	 * provides the requisite [[monocle.Getter]]s the scenario needs to function
	 * and serves as the ''SourceT'' for producing the desired
	 * [[com.github.osxhacker.demo.company.domain.Company]].
	 */
	@Lenses ()
	final private[scenario] case class ProfileSource (
		val event : CompanyProfileChanged,
		val company : Company
		)
	{
		def apply () : Company =
			Company.name
				.replace (event.name)
				.andThen (
					Company.description
						.replace (event.description) (_)
					) (company)
	}
}

