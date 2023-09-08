package com.github.osxhacker.demo.storageFacility.domain.scenario

import cats.MonadThrow
import org.typelevel.log4cats
import org.typelevel.log4cats.Logger

import com.github.osxhacker.demo.chassis
import com.github.osxhacker.demo.chassis.domain.Specification
import com.github.osxhacker.demo.chassis.domain.error.ValidationError
import com.github.osxhacker.demo.chassis.domain.event.{
	EventPolicy,
	Region
	}

import com.github.osxhacker.demo.chassis.effect.Pointcut
import com.github.osxhacker.demo.chassis.monitoring.metrics.UseCaseScenario
import com.github.osxhacker.demo.storageFacility.domain.{
	CanModify,
	ScopedEnvironment,
	StorageFacility
	}

import com.github.osxhacker.demo.storageFacility.domain.event.{
	AllStorageFacilityEvents,
	StorageFacilityDeleted
	}

import com.github.osxhacker.demo.storageFacility.domain.specification.RegionIs


/**
 * The '''DeleteFacility''' type defines the Use-Case scenario responsible for
 * deleting an existing
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]].  The
 * default configuration enforces the following conditions hold in order for the
 * Use-Case to succeed:
 *
 *   - The facility exists within the persistent store.
 *
 *   - The facility is owned by the `tenant`.
 *
 *   - The facility is defined in the service's
 *     [[com.github.osxhacker.demo.chassis.domain.event.Region]], __or__ the
 *     there is no [[com.github.osxhacker.demo.chassis.domain.event.Region]]
 *     for the
 *     [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]],
 *     __or__ "multi-region" support has been enabled.
 *
 * Of note in the implementation is that an additional, contextual, event
 * production decision is made here (as opposed to other Use-Cases) due to
 * [[com.github.osxhacker.demo.storageFacility.domain.scenario.DeleteCompany]]
 * being "cascading delete."
 */
sealed abstract class DeleteFacility[F[_]] private ()
	(
		implicit

		/// Needed for `flatMap` and `liftTo`.
		private val monadThrow : MonadThrow[F],

		/// Needed for `Aspect`.
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
	import InferFacilityChangeReport.HavingDeleted
	import cats.syntax.all._
	import chassis.syntax._
	import log4cats.syntax._
	import mouse.boolean._


	/// Instance Properties
	protected val allowDeletion : CanModify
	private val isSameRegion
		: ScopedEnvironment[F] => Specification[StorageFacility] =
		env => RegionIs (env.region) {
			StorageFacility.primary
				.asGetter
				.map (_.getOrElse (env.region))
				.map (Region.value.get)
			}


	override def toString () : String = "scenario: delete facility"


	final def apply (facility : StorageFacility)
		(implicit env : ScopedEnvironment[F])
		: F[Boolean] =
		delete (facility).addEventWhen (deletedInSameRegion ()) (
			StorageFacilityDeleted (facility)
			)
			.broadcast ()
			.map (_.isDefined)
			.measure[UseCaseScenario[F, DeleteFacility[F], Boolean]] ()


	/**
	 * The enableMultiRegion method configures a new '''DeleteFacility'''
	 * instance to allow invocations to proceed on more than just for the
	 * [[com.github.osxhacker.demo.chassis.domain.event.Region]] a service is
	 * deployed.
	 */
	final def enableMultiRegion () : DeleteFacility[F] =
		new DeleteFacility[F] () {
			override protected val allowDeletion = CanModify.AlwaysAllow
			}


	private def delete (facility : StorageFacility)
		(implicit env : ScopedEnvironment[F])
		: F[Option[StorageFacility]] =
		for {
			implicit0 (logger : Logger[F]) <- env.loggingFactory.create

			_ <- debug"${toString ()} - ${facility.id.show}"
			_ <- ValidateFacilityOwnership (facility, env.tenant).toEither
				.leftMap (ValidationError[StorageFacility])
				.liftTo[F]

			_ <- allowDeletion (facility)
			result <- env.storageFacilities
				.delete (facility)
				.flatTap (
					_.fold (
						InferFacilityChangeReport (HavingDeleted (facility)),
						monadThrow.unit
						)
					)

			_ <- debug"${toString ()} - ${facility.id.show} deleted? $result"
			} yield result.option (facility)


	private def deletedInSameRegion ()
		(implicit env : ScopedEnvironment[F])
		: Specification[Option[StorageFacility]] =
		Specification (_.exists (isSameRegion (env)))
}


object DeleteFacility
{
	/**
	 * The apply method is provided to allow functional-style creation of a
	 * '''DeleteFacility''' having the default
	 * [[com.github.osxhacker.demo.chassis.domain.event.Region]] policy within
	 * the context of ''F''.
	 */
	def apply[F[_]] ()
		(
			implicit
			monadThrow : MonadThrow[F],
			pointcut : Pointcut[F],
			policy: EventPolicy[
				F,
				ScopedEnvironment[F],
				AllStorageFacilityEvents
				]
		)
		: DeleteFacility[F] =
		new DeleteFacility[F] () {
			override protected val allowDeletion = CanModify.PrimaryRegion
			}
}

