package com.github.osxhacker.demo.storageFacility.domain.event

import monocle.Getter

import com.github.osxhacker.demo.chassis.domain.entity.Identifier
import com.github.osxhacker.demo.chassis.domain.event.{
	Region,
	ServiceFingerprint
	}

import com.github.osxhacker.demo.chassis.monitoring.CorrelationId
import com.github.osxhacker.demo.storageFacility.domain.{
	Company,
	StorageFacility
	}


/**
 * The '''StorageFacilityEvent''' `trait` defines the Domain Object Model common
 * ancestor to __all__
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]] domain
 * events known to the storage-facility microservice.
 */
trait StorageFacilityEvent
{
	/// Instance Properties
	val region : Region
	val fingerprint : Option[ServiceFingerprint]
	val correlationId : CorrelationId
	val id : Identifier[StorageFacility]
	val owner : Identifier[Company]
}


object StorageFacilityEvent
{
	/// Instance Properties
	val correlationId = Getter[StorageFacilityEvent, CorrelationId] (
		_.correlationId
		)

	val fingerprint = Getter[StorageFacilityEvent, Option[ServiceFingerprint]] (
		_.fingerprint
		)

	val id = Getter[StorageFacilityEvent, Identifier[StorageFacility]] (_.id)
	val owner = Getter[StorageFacilityEvent, Identifier[Company]] (_.owner)
	val region = Getter[StorageFacilityEvent, Region] (_.region)
}

