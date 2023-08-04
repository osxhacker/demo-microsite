package com.github.osxhacker.demo.storageFacility.domain.event

import io.scalaland.chimney
import monocle.macros.Lenses

import com.github.osxhacker.demo.storageFacility.domain._
import com.github.osxhacker.demo.chassis.domain.entity.Identifier
import com.github.osxhacker.demo.chassis.domain.event.{
	Region,
	ServiceFingerprint
	}

import com.github.osxhacker.demo.chassis.monitoring.CorrelationId


/**
 * The '''StorageFacilityDeleted''' type is the Domain Object Model
 * representation of an event which is emitted when a
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]] has been
 * successfully removed from the persistent store.
 */
@Lenses ()
final case class StorageFacilityDeleted (
	override val region : Region,
	override val fingerprint : Option[ServiceFingerprint],
	override val correlationId : CorrelationId,
	override val id : Identifier[StorageFacility],
	override val owner : Identifier[Company]
	)
	extends StorageFacilityEvent


object StorageFacilityDeleted
{
	/// Class Imports
	import chimney.dsl._


	/**
	 * This version of the apply method is provided to allow functional-style
	 * creation from the '''facility''' and available `implicit`
	 * [[com.github.osxhacker.demo.storageFacility.domain.ScopedEnvironment]].
	 */
	def apply[F[_]] (facility : StorageFacility)
		(implicit env : ScopedEnvironment[F])
		: StorageFacilityDeleted =
		facility.into[StorageFacilityDeleted]
			.withFieldConst (_.correlationId, env.correlationId)
			.withFieldConst (_.fingerprint, None)
			.withFieldConst (_.region, env.region)
			.withFieldComputed (
				_.owner,
				StorageFacility.owner
					.andThen (Company.id)
					.get
				)
			.transform
}

