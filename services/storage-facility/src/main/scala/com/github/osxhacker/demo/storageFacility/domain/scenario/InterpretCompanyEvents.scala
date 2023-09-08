package com.github.osxhacker.demo.storageFacility.domain.scenario

import cats.data.Kleisli
import eu.timepit.refined
import org.typelevel.log4cats
import org.typelevel.log4cats.{
	LoggerFactory,
	StructuredLogger
	}

import com.github.osxhacker.demo.chassis.domain.error.{
	ObjectNotFoundError,
	StaleObjectError
	}

import com.github.osxhacker.demo.chassis.effect.Pointcut
import com.github.osxhacker.demo.chassis.monitoring.Subsystem
import com.github.osxhacker.demo.chassis.domain.repository.{
	CreateIntent,
	UpdateIntent
	}

import com.github.osxhacker.demo.storageFacility.domain._
import com.github.osxhacker.demo.storageFacility.domain.event._


/**
 * The '''InterpretCompanyEvents''' type defines the Domain Object Model
 * Use-Case scenario responsible for interpreting
 * [[com.github.osxhacker.demo.storageFacility.domain.Company]]-related
 * [[com.github.osxhacker.demo.storageFacility.domain.event]]s.
 */
final case class InterpretCompanyEvents[F[_]] ()
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
	extends AbstractEventInterpreter[F, Company, AllCompanyEvents] ()
{
	/// Class Imports
	import InferCompanyChangeReport._
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
			: Case.Aux[CompanyCreated, F[Unit]] =
			handler[CompanyCreated] ("create company: " + _.slug.show) {
				event =>
					for {
						_ <- debug"creating company: slug=${event.slug.show}"
						company <- scenarios.save[CreateIntent] (
							event.toCompany ()
							)
							.flatTap {
								saved =>
									InferCompanyChangeReport (
										HavingCreated (saved)
										)
								}

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
						company <- scenarios.find (event.id)

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
			handler[CompanyProfileChanged] ("change profile" + _.id.show) {
				event =>
					for {
						_ <- debug"resolving company: id=${event.id.show}"
						existing <- scenarios.find (event.id)

						_ <- debug"modifying profile: id=${existing.id.show}"
						updated <- scenarios.save[UpdateIntent] (
							Company.name.replace (event.name) (existing)
							)

						unit <- debug"profile altered: ${updated.slug.show}"
						} yield unit
				}


		implicit def caseSlugChanged (
			implicit
			env : ScopedEnvironment[F],
			logger : StructuredLogger[F]
			)
			: Case.Aux[CompanySlugChanged, F[Unit]] =
			handler[CompanySlugChanged] ("change slug" + _.from.show) {
				event =>
					for {
						_ <- debug"resolving company: id=${event.id.show}"
						existing <- scenarios.find (event.id)

						_ <- debug"modifying slug: id=${existing.id.show}"
						_ <- scenarios.save[UpdateIntent] (
							Company.slug.replace (event.to) (existing)
							)
							.flatTap {
								saved =>
									InferCompanyChangeReport (
										HavingModified (existing, saved)
										)
								}

						unit <- debug"slug changed: ${event.from.show} -> ${event.to.show}"
					} yield unit
			}


		implicit def caseStatusChanged (
			implicit
			env : ScopedEnvironment[F],
			logger : StructuredLogger[F]
			)
			: Case.Aux[CompanyStatusChanged, F[Unit]] =
			handler[CompanyStatusChanged] ("change status" + _.id.show) {
				event =>
					for {
						_ <- debug"resolving company: id=${event.id.show}"
						existing <- scenarios.find (event.id)

						_ <- debug"modifying status: id=${existing.id.show}"
						_ <- scenarios.save[UpdateIntent] (
							Company.status.replace (event.status) (existing)
							)
							.flatTap {
								saved =>
									InferCompanyChangeReport (
										HavingModified (existing, saved)
										)
								}

						unit <- debug"status changed: ${event.status.show}"
					} yield unit
			}
	}


	private object scenarios
	{
		/// Instance Properties
		val delete = DeleteCompany[F] ()
		val find = FindCompany[F] ()
		val save = SaveCompany[F] ()
	}


	private object scopeEnvironment
		extends MakeScopedEnvironment ()
	{
		/// Implicit Conversions
		implicit def caseEvent[EventT <: CompanyEvent] (
			implicit global : GlobalEnvironment[F]
			)
			: Case.Aux[EventT, F[ScopedEnvironment[F]]] =
			definition[CompanyEvent, EventT] (
				CompanyEvent.id,
				CompanyEvent.correlationId,
				CompanyEvent.id,
				CompanyEvent.region
				)
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
							interpret (event)(env, logger)
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

