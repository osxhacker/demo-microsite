package com.github.osxhacker.demo.storageFacility.domain.scenario

import scala.language.postfixOps

import cats.data.Kleisli
import eu.timepit.refined
import monocle.{
	syntax => _,
	_
	}

import monocle.macros.GenLens
import shapeless.{
	syntax => _,
	_
	}

import com.github.osxhacker.demo.chassis
import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.chassis.domain.event.{
	EventPolicy,
	Region
	}

import com.github.osxhacker.demo.chassis.effect.Pointcut
import com.github.osxhacker.demo.chassis.monitoring.metrics.UseCaseScenario
import com.github.osxhacker.demo.storageFacility.domain.{
	Company,
	ScopedEnvironment,
	StorageFacility
	}

import com.github.osxhacker.demo.storageFacility.domain.event.AllStorageFacilityEvents


/**
 * The '''AddNewFacility''' type defines the Use-Case scenario responsible for
 * associating a
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]] with an
 * active [[com.github.osxhacker.demo.storageFacility.domain.Company]] if an
 * identical one does not already exist.  Conceptually, this Use-Case is at a
 * higher level than those it uses by being closer to the problem domain.
 */
final case class AddNewFacility[F[_], SourceT <: AnyRef] private (
	private val create : CreateFacility[F, SourceT :: Region :: Company :: HNil]
	)
	(
		implicit

		/// Needed for `compile` and `liftTo`.
		private val compiler : fs2.Compiler.Target[F],

		/// Needed for `measure`.
		private val pointcut : Pointcut[F]
	)
{
	/// Class Imports
	import cats.syntax.all._
	import chassis.syntax._


	/// Instance Properties
	private val findActiveCompany = FindActiveCompany[F] ()


	override def toString () : String = "scenario: add new facility"


	def apply (source : SourceT)
		(implicit env : ScopedEnvironment[F])
		: F[StorageFacility] =
		add (source).measure[
			UseCaseScenario[F, AddNewFacility[F, SourceT], StorageFacility]
			] ()


	private def add (source : SourceT)
		(implicit env : ScopedEnvironment[F])
		: F[StorageFacility] =
		findActiveCompany (env.tenant).map (source :: env.region :: _ :: HNil)
			.flatMap (create (_))
}


object AddNewFacility
{
	/// Class Imports
	import refined.api.Refined


	/// Class Types
	private final type FactoryParamsType[A] = A :: Region :: Company :: HNil


	final class PartiallyApplied[F[_]] ()
	{
		def apply[SourceT <: AnyRef, T, P1, P2] (
			available : Getter[SourceT, Refined[T, P1]],
			capacity : Getter[SourceT, Refined[T, P2]]
			)
			(factory : Kleisli[ErrorOr, FactoryParamsType[SourceT], StorageFacility])
			(
				implicit
				compiler : fs2.Compiler.Target[F],
				numeric : Numeric[T],
				pointcut : Pointcut[F],
				policy : EventPolicy[
					F,
					ScopedEnvironment[F],
					AllStorageFacilityEvents
					]
			)
			: AddNewFacility[F, SourceT] =
			new AddNewFacility[F, SourceT] (
				CreateFacility[F] (
					GenLens[FactoryParamsType[SourceT]] (_.head) andThen available,
					GenLens[FactoryParamsType[SourceT]] (_.head) andThen capacity
					) (factory)
				)
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
