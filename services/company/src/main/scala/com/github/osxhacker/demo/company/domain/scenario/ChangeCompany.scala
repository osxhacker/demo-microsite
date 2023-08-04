package com.github.osxhacker.demo.company.domain.scenario

import scala.language.postfixOps
import scala.reflect.ClassTag

import cats.MonadThrow
import cats.data.{
	Kleisli,
	ValidatedNec
	}

import eu.timepit.refined.api.Refined
import monocle._

import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.chassis.domain.entity.{
	Identifier,
	Version
	}

import com.github.osxhacker.demo.chassis.domain.error.{
	LogicError,
	StaleObjectError,
	ValidationError
	}

import com.github.osxhacker.demo.chassis
import com.github.osxhacker.demo.chassis.domain.Slug
import com.github.osxhacker.demo.chassis.domain.event.{
	EventLog,
	EventPolicy
	}

import com.github.osxhacker.demo.chassis.domain.repository.UpdateIntent
import com.github.osxhacker.demo.chassis.effect.Pointcut
import com.github.osxhacker.demo.chassis.monitoring.metrics.UseCaseScenario

import com.github.osxhacker.demo.company.domain._
import com.github.osxhacker.demo.company.domain.event._


/**
 * The '''ChangeCompany''' type defines the Use-Case scenario responsible for
 * validating the information known to produce an
 * [[com.github.osxhacker.demo.chassis.domain.repository.Intent]] applicable for
 * modifying an existing [[com.github.osxhacker.demo.company.domain.Company]].
 *
 * Much of the work done in this Use-Case scenario is performed in the
 * [[com.github.osxhacker.demo.chassis.domain.ErrorOr]] [[cats.MonadThrow]]
 * container as it is a lightweight container.
 */
final case class ChangeCompany[F[_], SourceT <: AnyRef] (
	private val id : AdaptOptics.KleisliType[SourceT, Identifier[Company]],
	private val version : AdaptOptics.KleisliType[SourceT, Version],
	private val slug : AdaptOptics.KleisliType[SourceT, Slug],
	private val status : AdaptOptics.KleisliType[SourceT, CompanyStatus]
	)
	(private val factory : Kleisli[ErrorOr, SourceT, Company])
	(
		implicit

		/// Needed for `flatMap` and `liftTo`.
		private val monadThrow : MonadThrow[F],

		/// Needed for '''ValidationError'''.
		private val classTag : ClassTag[SourceT],

		/// Needed for `measure`.
		private val pointcut : Pointcut[F],

		/// Needed for `broadcast`.
		private val policy : EventPolicy[
			F,
			ScopedEnvironment[F],
			AllCompanyEvents
			]
	)
{
	/// Class Imports
	import cats.syntax.all._
	import chassis.syntax._
	import mouse.boolean._


	/// Instance Properties
	private val save = SaveCompany[F] ()


	override def toString () : String = "scenario: change company"


	def apply (existing : Company, source : SourceT)
		(implicit env : ScopedEnvironment[F])
		: F[Company] =
		change (existing, source).broadcast ()
			.measure[
				UseCaseScenario[
					F,
					ChangeCompany[F, SourceT],
					Company
					]
				] ()


	private def change (existing : Company, source : SourceT)
		(implicit env : ScopedEnvironment[F])
		: EventLog[F, Company, AllCompanyEvents] =
		(validate () andThen factory)
			.map (UpdateIntent (_).filter (_.differsFrom (existing)))
			.run (existing -> source)
			.liftTo[EventLog[F, *, AllCompanyEvents]]
			.flatMap {
				intent =>
					EventLog.liftF (save (intent))
				}
			.mapBoth {
				case (events, Some (saved)) =>
					(
						events |+| existing.infer[CompanyChangeEvents] (saved)
							.toEvents ()
							.map (_.embed[AllCompanyEvents]),

						saved
					)

				case (events, None) =>
					events -> existing
				}


	private def checkForLogicErrors (existing : Company, source : SourceT)
		: ErrorOr[Version] =
		id (source).andThen {
			submitted =>
				(submitted === existing.id).validatedNec (
					"company id's do not match",
					submitted
				)
			}
			.productR (version (source))
			.leftMap (errs => LogicError (errs.head))
			.toEither


	private def checkFutureVersion (existing : Company, source : SourceT)
		: ValidatedNec[String, Version] =
		version (source) andThen {
			submitted =>
				(submitted <= existing.version).validatedNec (
					s"'future' company version detected: ${submitted.show}",
					submitted
					)
			}


	private def checkStaleVersion (existing : Company)
		(submitted : Version)
		: ErrorOr[Unit] =
		(submitted >= existing.version).liftTo[ErrorOr] (
			StaleObjectError[Company] (
				existing.id,
				submitted,
				latest = existing.some
				)
			)


	private def checkStatusChange (existing : Company, source : SourceT)
		: ValidatedNec[String, CompanyStatus] =
		status (source) andThen {
			submitted =>
				existing.status
					.canBecome (submitted)
					.validatedNec (
						s"invalid status change detected: ${submitted.show}",
						submitted
						)
				}


	private def validate ()
		(implicit env : ScopedEnvironment[F])
		: Kleisli[ErrorOr, (Company, SourceT), SourceT] =
		Kleisli {
			case (existing, source) =>
				(
					checkForLogicErrors (existing, source) >>=
					checkStaleVersion (existing)
				) >>
				(
					ValidateSlug (source, slug) *>
					checkFutureVersion (existing, source) *>
					checkStatusChange (existing, source)
				)
					.bimap (ValidationError[Company](_), _ => source)
					.toEither
			}
}


object ChangeCompany
{
	/// Class Types
	final class PartiallyApplied[F[_]] ()
	{
		def apply[SourceT <: AnyRef, IdT, P1, P2, StatusT <: AnyRef] (
			id : Getter[SourceT, IdT],
			version : Getter[SourceT, Refined[Int, P1]],
			slug : Getter[SourceT, Refined[String, P2]],
			status : Getter[SourceT, StatusT]
			)
			(factory : Kleisli[ErrorOr, SourceT, Company])
			(
				implicit
				monadThrow : MonadThrow[F],
				classTag : ClassTag[SourceT],
				parser : Identifier.Parser[Company, IdT],
				pointcut : Pointcut[F],
				policy : EventPolicy[F, ScopedEnvironment[F], AllCompanyEvents]
			)
			: ChangeCompany[F, SourceT] =
			new ChangeCompany[F, SourceT] (
				id = AdaptOptics.id (id),
				version = AdaptOptics.version (version),
				slug = AdaptOptics.slug (slug),
				status = AdaptOptics.status (status)
				) (factory)
	}


	/**
	 * The apply method is provided to support functional-style creation and
	 * employs the "partially applied" idiom, thus only requiring collaborators
	 * to provide ''F'' and allowing the compiler to deduce the remaining type
	 * parameters.
	 */
	@inline
	def apply[F[_]] : PartiallyApplied[F] = new PartiallyApplied[F] ()
}

