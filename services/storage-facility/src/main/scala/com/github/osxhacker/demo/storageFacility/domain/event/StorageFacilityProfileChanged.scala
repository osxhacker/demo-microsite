package com.github.osxhacker.demo.storageFacility.domain.event

import io.scalaland.chimney
import monocle.macros.Lenses

import com.github.osxhacker.demo.chassis.domain.Slug
import com.github.osxhacker.demo.chassis.domain.entity._
import com.github.osxhacker.demo.chassis.domain.event.{
	Region,
	ServiceFingerprint
	}

import com.github.osxhacker.demo.chassis.monitoring.CorrelationId
import com.github.osxhacker.demo.storageFacility.domain._


/**
 * The '''StorageFacilityProfileChanged''' type is the Domain Object Model
 * representation of an event which is emitted when a
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]]
 * has its ancillary properties altered.
 */
@Lenses ()
final case class StorageFacilityProfileChanged (
	override val region : Region,
	override val fingerprint : Option[ServiceFingerprint],
	override val correlationId : CorrelationId,
	override val id : Identifier[StorageFacility],
	override val owner : Identifier[Company],
	val name : StorageFacility.Name,
	val city : StorageFacility.City,
	val state : StorageFacility.State,
	val zip : StorageFacility.Zip
	)
	extends StorageFacilityEvent


object StorageFacilityProfileChanged
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
		: StorageFacilityProfileChanged =
		facility.into[StorageFacilityProfileChanged]
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

