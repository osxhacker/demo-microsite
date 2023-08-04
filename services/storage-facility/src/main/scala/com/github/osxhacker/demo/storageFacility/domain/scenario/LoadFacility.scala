package com.github.osxhacker.demo.storageFacility.domain.scenario

import scala.language.postfixOps

import cats.MonadThrow
import monocle.Getter

import com.github.osxhacker.demo.chassis
import com.github.osxhacker.demo.chassis.domain.entity.Identifier
import com.github.osxhacker.demo.chassis.domain.error.ValidationError
import com.github.osxhacker.demo.chassis.effect.{
	Aspect,
	Pointcut
	}

import com.github.osxhacker.demo.chassis.monitoring.metrics.UseCaseScenario
import com.github.osxhacker.demo.storageFacility.domain.{
	Company,
	ScopedEnvironment,
	StorageFacility
	}


/**
 * The '''LoadFacility''' type defines the Use-Case scenario responsible for
 * resolving a
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]] based
 * only on its
 * [[com.github.osxhacker.demo.chassis.domain.entity.Identifier]].  If it does
 * not exist or there is a problem retrieving it, an error is raised in ''F''.
 *
 * This scenario is defined in terms of
 * [[com.github.osxhacker.demo.storageFacility.domain.scenario.FindActiveCompany]]
 * and
 * [[com.github.osxhacker.demo.storageFacility.domain.scenario.FindFacility]],
 */
final class LoadFacility[F[_], SourceT <: AnyRef, IdT] private (
	private val id : Getter[SourceT, IdT]
	)
	(
		implicit

		/// Needed for `ensureOr`.
		private val monadThrow : MonadThrow[F],

		/// Needed for `FindFacility`.
		parser : Identifier.Parser[StorageFacility, IdT],

		/// Needed for `measure`.
		private val pointcut : Pointcut[F]
	)
{
	/// Class Imports
	import cats.syntax.all._
	import chassis.syntax._


	/// Instance Properties
	implicit lazy val cachedAspect = Aspect[
		F,
		UseCaseScenario[F, LoadFacility[F, SourceT, IdT], StorageFacility]
		].static ()

	private val findCompany = FindActiveCompany[F] ()
	private val findFacility = FindFacility[F] (id)


	override def toString () : String =
		"scenario: load facility with active company"


	def apply (source : SourceT)
		(implicit env : ScopedEnvironment[F])
		: F[StorageFacility] =
		load (source).measure[
			UseCaseScenario[
				F,
				LoadFacility[F, SourceT, IdT],
				StorageFacility
				]
			] ()


	private def load (source : SourceT)
		(implicit env : ScopedEnvironment[F])
		: F[StorageFacility] =
		for {
			tenant <- findCompany (env.tenant)
			facility <- findFacility (source)
			result <- verifyOwnership (facility, tenant)
			} yield result


	private def verifyOwnership (facility : StorageFacility, owner : Company)
		: F[StorageFacility] =
		ValidateFacilityOwnership (facility, owner)
			.leftMap (ValidationError[StorageFacility])
			.toEither
			.liftTo[F]
}


object LoadFacility
{
	/// Class Types
	final class PartiallyApplied[F[_]]
	{
		def apply[SourceT <: AnyRef, IdT] (id : Getter[SourceT, IdT])
			(
				implicit
				monadThrow : MonadThrow[F],
				parser : Identifier.Parser[StorageFacility, IdT],
				pointcut : Pointcut[F]
			)
			: LoadFacility[F, SourceT, IdT] =
			new LoadFacility[F, SourceT, IdT] (id)
	}


	/**
	 * The apply method is provided to support functional-style creation and
	 * employs the "partially applied" idiom, thus only requiring collaborators
	 * to provide ''F'' and allow the compiler to deduce the remaining type
	 * parameters.
	 */
	@inline
	def apply[F[_]] : PartiallyApplied[F] = new PartiallyApplied[F]()
}
