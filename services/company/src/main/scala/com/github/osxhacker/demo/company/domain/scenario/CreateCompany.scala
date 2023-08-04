package com.github.osxhacker.demo.company.domain.scenario

import scala.language.postfixOps
import scala.reflect.ClassTag

import cats.MonadThrow
import cats.data.Kleisli
import eu.timepit.refined.api.Refined
import monocle.Getter

import com.github.osxhacker.demo.chassis
import com.github.osxhacker.demo.chassis.domain.{
	ErrorOr,
	Slug
	}

import com.github.osxhacker.demo.chassis.domain.error._
import com.github.osxhacker.demo.chassis.domain.event.{
	EventLog,
	EventPolicy
	}

import com.github.osxhacker.demo.chassis.domain.repository.CreateIntent
import com.github.osxhacker.demo.chassis.effect.Pointcut
import com.github.osxhacker.demo.chassis.monitoring.metrics.UseCaseScenario
import com.github.osxhacker.demo.company.domain.{
	Company,
	ScopedEnvironment
	}

import com.github.osxhacker.demo.company.domain.event.{
	AllCompanyEvents,
	CompanyCreated
	}


/**
 * The '''CreateCompany''' type defines the Use-Case scenario responsible for
 * validating the information known to produce an
 * [[com.github.osxhacker.demo.chassis.domain.repository.Intent]] applicable to
 * create a [[com.github.osxhacker.demo.company.domain.Company]] and then
 * invoking
 * [[com.github.osxhacker.demo.company.domain.scenario.SaveCompany]] to
 * persist it.
 */
final class CreateCompany[F[_], SourceT <: AnyRef] (
	private val slug : AdaptOptics.KleisliType[SourceT, Slug]
	)
	(factory : Kleisli[ErrorOr, SourceT, Company])
	(
		implicit

		/// Needed for ValidationError.
		private val classTag : ClassTag[SourceT],

		/// Needed for `flatMap` and `liftTo`.
		private val monadThrow : MonadThrow[F],

		/// Needed for `measure`.
		private val pointcut : Pointcut[F],

		/// Needed for `broadcast`.
		private val policy : EventPolicy[F, ScopedEnvironment[F], AllCompanyEvents]
	)
{
	/// Class Imports
	import cats.syntax.all._
	import chassis.syntax._
	import mouse.foption._


	/// Instance Properties
	private val save = SaveCompany[F] ()


	override def toString () : String = "scenario: create company"


	def apply (source : SourceT)
		(implicit env : ScopedEnvironment[F])
		: F[Company] =
		create (source).broadcast ()
			.measure[
				UseCaseScenario[
					F,
					CreateCompany[F, SourceT],
					Company
					]
				] ()


	private def create (source : SourceT)
		(implicit env : ScopedEnvironment[F])
		: EventLog[F, Company, AllCompanyEvents] =
		mkCompany (source).flatMap {
			company =>
				save (CreateIntent (company)).getOrRaise (
					LogicError ("save did not produce a company")
					)
			}
			.deriveEvent (CompanyCreated (_))


	private def mkCompany (source : SourceT)
		(implicit env : ScopedEnvironment[F])
		: F[Company] =
		(validate (source) >>= factory.run).liftTo[F]


	private def validate (source : SourceT)
		(implicit env : ScopedEnvironment[F])
		: ErrorOr[SourceT] =
		ValidateSlug (source, slug).bimap (
			ValidationError[SourceT],
			_ => source
			)
			.toEither
}


object CreateCompany
{
	/// Class Types
	final class PartiallyApplied[F[_]] ()
	{
		def apply[SourceT <: AnyRef, P1] (
			slug : Getter[SourceT, Refined[String, P1]]
			)
			(factory : Kleisli[ErrorOr, SourceT, Company])
			(
				implicit
				classTag : ClassTag[SourceT],
				monadThrow : MonadThrow[F],
				pointcut : Pointcut[F],
				policy : EventPolicy[F, ScopedEnvironment[F], AllCompanyEvents]
			)
			: CreateCompany[F, SourceT] =
			new CreateCompany[F, SourceT] (AdaptOptics.slug (slug)) (factory)
	}


	/**
	 * The apply method is provided to support functional-style creation and
	 * employs the "partially applied" idiom, thus only requiring collaborators
	 * to provide ''F'' and allowing the compiler to deduce the remaining type
	 * parameters.
	 */
	@inline
	def apply[F[_]]: PartiallyApplied[F] = new PartiallyApplied[F]()
}

