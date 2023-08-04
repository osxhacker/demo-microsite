package com.github.osxhacker.demo.storageFacility.domain.scenario

import scala.reflect.ClassTag

import cats.MonadThrow
import eu.timepit.refined.api.Refined
import monocle.{
	Getter,
	Iso
	}

import com.github.osxhacker.demo.chassis
import com.github.osxhacker.demo.chassis.domain.entity.Version
import com.github.osxhacker.demo.chassis.domain.error.{
	StaleObjectError,
	ValidationError
	}

import com.github.osxhacker.demo.chassis.domain.event.{
	EventLog,
	EventPolicy
	}

import com.github.osxhacker.demo.chassis.domain.repository.UpdateIntent
import com.github.osxhacker.demo.chassis.effect.Pointcut
import com.github.osxhacker.demo.chassis.monitoring.metrics.UseCaseScenario
import com.github.osxhacker.demo.storageFacility.domain.{
	ScopedEnvironment,
	StorageFacility,
	StorageFacilityStatus
	}

import com.github.osxhacker.demo.storageFacility.domain.event.{
	AllStorageFacilityEvents,
	StorageFacilityStatusChanged
	}


/**
 * The '''ChangeFacilityStatus''' type defines the Domain Object Model Use-Case
 * scenario responsible for altering a
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]] status
 * (if allowed) and then invoking
 * [[com.github.osxhacker.demo.storageFacility.domain.scenario.SaveFacility]] to
 * persist it.
 */
final case class ChangeFacilityStatus[F[_], SourceT] (
	private val version : AdaptOptics.KleisliType[SourceT, Version]
	)
	(
		implicit

		/// Needed for ''ValidationError''.
		private val classTag : ClassTag[SourceT],

		/// Needed for `flatMap` and `save`.
		private val monadThrow : MonadThrow[F],

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
	import cats.syntax.all._
	import chassis.syntax._
	import mouse.foption._


	/// Instance Properties
	private val save = SaveFacility[F] ()


	override def toString () : String = "scenario: change facility status"


	def apply (
		existing : StorageFacility,
		versionSource : SourceT,
		status : StorageFacilityStatus
		)
		(implicit env : ScopedEnvironment[F])
		: F[StorageFacility] =
		changeStatus (existing, versionSource, status).broadcast ()
			.measure[
				UseCaseScenario[
					F,
					ChangeFacilityStatus[F, SourceT],
					StorageFacility
					]
				] ()


	private def changeStatus (
		existing : StorageFacility,
		versionSource : SourceT,
		desired : StorageFacilityStatus
		)
		(implicit env : ScopedEnvironment[F])
		: EventLog[F, StorageFacility, AllStorageFacilityEvents] =
		version.mapF {
			_.leftMap (ValidationError[SourceT] (_))
				.toEither
				.liftTo[F]
			}
			.second[StorageFacility]
			.mapF[EventLog[F, *, AllStorageFacilityEvents], StorageFacility] {
				_.flatMap ((updateInstanceAndSave (desired) _).tupled)
					.deriveEvent (StorageFacilityStatusChanged (_))
				}
			.run (existing -> versionSource)


	private def updateInstanceAndSave (desired : StorageFacilityStatus)
		(existing : StorageFacility, version : Version)
		(implicit env: ScopedEnvironment[F])
		: F[StorageFacility] =
		existing.changeStatusTo[F] (desired)
			.map (UpdateIntent (_).filter (_.version === version))
			.flatMap {
				save (_).getOrRaise (
					StaleObjectError[StorageFacility] (
						existing.id,
						existing.version,
						existing.some
						)
					)
				}
}


object ChangeFacilityStatus
{
	/// Class Types
	final class PartiallyApplied[F[_]] ()
	{
		/**
		 * This version of the apply method constructs a
		 * '''ChangeStorageFacilityStatus''' scenario which has as its ''SourceT'' the
		 * [[com.github.osxhacker.demo.chassis.domain.entity.Version]] required.
		 */
		def apply ()
			(
				implicit
				monadThrow : MonadThrow[F],
				pointcut : Pointcut[F],
				policy : EventPolicy[
					F,
					ScopedEnvironment[F],
					AllStorageFacilityEvents
					]
			)
			: ChangeFacilityStatus[F, Version] =
			new ChangeFacilityStatus[F, Version] (AdaptOptics (Iso.id))


		/**
		 * This version of the apply method constructs a scenario which attempts
		 * to use a [[com.github.osxhacker.demo.chassis.domain.entity.Version]]
		 * within an arbitrary ''SourceT''.
		 */
		def apply[SourceT, P1] (version : Getter[SourceT, Refined[Int, P1]])
			(
				implicit
				classTag : ClassTag[SourceT],
				monadThrow : MonadThrow[F],
				pointcut : Pointcut[F],
				policy : EventPolicy[
					F,
					ScopedEnvironment[F],
					AllStorageFacilityEvents
					]
			)
			: ChangeFacilityStatus[F, SourceT] =
			new ChangeFacilityStatus[F, SourceT] (AdaptOptics.version (version))
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
