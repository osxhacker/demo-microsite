package com.github.osxhacker.demo.storageFacility.domain.scenario

import scala.language.postfixOps

import cats.data.{
	Kleisli,
	OptionT
	}

import com.softwaremill.diffx.Diff
import eu.timepit.refined
import monocle._
import squants.space.Volume

import com.github.osxhacker.demo.chassis
import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.chassis.domain.error._
import com.github.osxhacker.demo.chassis.domain.event.{
	EventLog,
	EventPolicy
	}

import com.github.osxhacker.demo.chassis.domain.repository.CreateIntent
import com.github.osxhacker.demo.chassis.effect.Pointcut
import com.github.osxhacker.demo.chassis.monitoring.metrics.UseCaseScenario
import com.github.osxhacker.demo.storageFacility.domain.{
	ScopedEnvironment,
	StorageFacility
	}

import com.github.osxhacker.demo.storageFacility.domain.event.{
	AllStorageFacilityEvents,
	StorageFacilityCreated
	}

import com.github.osxhacker.demo.storageFacility.domain.specification.FacilityNameIs


/**
 * The '''CreateFacility''' type defines the Use-Case scenario responsible for
 * validating the information known in order to produce an
 * [[com.github.osxhacker.demo.chassis.domain.repository.Intent]] applicable to
 * create a [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]]
 * and then invoking
 * [[com.github.osxhacker.demo.storageFacility.domain.scenario.SaveFacility]] to
 * persist it.
 *
 * Additionally, '''CreateFacility''' provides idempotent persistence by
 * supporting invocations having "identical domain-values" with a persisted
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]].
 */
final class CreateFacility[F[_], SourceT <: AnyRef] private (
	private val available : AdaptOptics.KleisliType[SourceT, Volume],
	private val capacity : AdaptOptics.KleisliType[SourceT, Volume]
	)
	(private val factory : Kleisli[ErrorOr, SourceT, StorageFacility])
	(
		implicit

		/// Needed for `compile` and `liftTo`.
		private val compiler : fs2.Compiler.Target[F],

		/// Needed for `measure`.
		private val pointcut : Pointcut[F],

		/// Needed for `broadcast`.
		private val policy : EventPolicy[
			F,
			ScopedEnvironment[F],
			AllStorageFacilityEvents
			]
	)
{
	/// Class Imports
	import CreateFacility.diffByDomainValues
	import cats.syntax.all._
	import chassis.syntax._
	import mouse.any._


	/// Instance Properties
	private val conflictingInstanceExists =
		ConflictingObjectsError[StorageFacility] (
			s"${toString ()} - unable to create due to conflicting properties"
			)

	private val save = SaveFacility[F] ()


	override def toString () : String = "scenario: create facility"


	def apply (source : SourceT)
		(implicit env : ScopedEnvironment[F])
		: F[StorageFacility] =
		create (source).broadcast ()
			.measure[
				UseCaseScenario[F, CreateFacility[F, SourceT], StorageFacility]
				] ()


	private def create (source : SourceT)
		(implicit env : ScopedEnvironment[F])
		: EventLog[F, StorageFacility, AllStorageFacilityEvents] =
		mkNewFacility (source).flatMap {
			unsaved =>
				findExistingBy (unsaved.name).orElseF (
					CreateIntent (unsaved) |> save.apply
					)
					.filter (hasIdenticalDomainValues (unsaved))
					.getOrRaise (conflictingInstanceExists)
			}
			.deriveEvent (StorageFacilityCreated (_))


	private def findExistingBy (name : StorageFacility.Name)
		(implicit env : ScopedEnvironment[F])
		: OptionT[F, StorageFacility] =
		OptionT {
			env.storageFacilities
				.queryBy (FacilityNameIs (name))
				.take (1)
				.compile
				.to (Array)
				.map (_.headOption)
			}


	private def hasIdenticalDomainValues (a : StorageFacility)
		: StorageFacility => Boolean =
		_.differsFrom (a) (diffByDomainValues) === false


	private def mkNewFacility (source : SourceT)
		: F[StorageFacility] =
		(validate (source) >>= factory.run).liftTo[F]


	private def validate (source : SourceT) : ErrorOr[SourceT] =
		ValidateStorageVolume (source, available, capacity).bimap (
			ValidationError[StorageFacility],
			_ => source
			)
			.toEither
}


object CreateFacility
{
	/// Class Imports
	import cats.syntax.either._
	import refined.api.Refined


	/// Class Types
	final class PartiallyApplied[F[_]] ()
	{
		/**
		 * This version of the apply method is provided to allow
		 * functional-style creation when the '''available''' and '''capacity'''
		 * properties are accessible with [[monocle.Getter]]s, which is the
		 * general case.
		 */
		def apply[SourceT <: AnyRef, T, P1, P2] (
			available : Getter[SourceT, Refined[T, P1]],
			capacity : Getter[SourceT, Refined[T, P2]]
			)
			(factory : Kleisli[ErrorOr, SourceT, StorageFacility])
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
			: CreateFacility[F, SourceT] =
			new CreateFacility[F, SourceT] (
				available = AdaptOptics.availability (available),
				capacity = AdaptOptics.capacity (capacity)
				) (factory)


		/**
		 * This version of the apply method is provided to allow
		 * functional-style creation when the '''available''' and '''capacity'''
		 * properties are accessible with [[cats.data.Kleisli]]s, which is the
		 * case when ''SourceT'' is contains a [[squants.space.Volume]] already.
		 * The possibility of an invalid [[squants.space.Volume]] is allowed for
		 * due to the [[cats.data.Kleisli]] being defined in terms of the
		 * [[com.github.osxhacker.demo.chassis.domain.ErrorOr]] container.
		 */
		def apply[SourceT <: AnyRef] (
			available : Kleisli[ErrorOr, SourceT, Volume],
			capacity : Kleisli[ErrorOr, SourceT, Volume]
			)
			(factory : Kleisli[ErrorOr, SourceT, StorageFacility])
			(
				implicit
				compiler : fs2.Compiler.Target[F],
				pointcut : Pointcut[F],
				policy : EventPolicy[
					F,
					ScopedEnvironment[F],
					AllStorageFacilityEvents
					]
			)
			: CreateFacility[F, SourceT] =
			new CreateFacility[F, SourceT] (
				available = available.mapF {
					_.leftMap (_.getMessage)
						.toValidatedNec
					},

				capacity = capacity.mapF {
					_.leftMap (_.getMessage)
						.toValidatedNec
					}
				) (factory)
	}


	/// Instance Properties
	private val diffByDomainValues : Diff[StorageFacility] =
		StorageFacility.storageFacilityDiff
			.ignore (_.id)
			.ignore (_.version)


	/**
	 * The apply method is provided to support functional-style creation and
	 * employs the "partially applied" idiom, thus only requiring collaborators
	 * to provide ''F'' and allow the compiler to deduce the remaining type
	 * parameters.
	 */
	@inline
	def apply[F[_]] : PartiallyApplied[F] = new PartiallyApplied[F] ()
}

