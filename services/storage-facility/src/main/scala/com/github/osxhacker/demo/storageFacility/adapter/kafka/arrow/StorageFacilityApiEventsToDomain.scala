package com.github.osxhacker.demo.storageFacility.adapter.kafka.arrow


import io.scalaland.chimney

import com.github.osxhacker.demo.chassis.adapter.kafka.arrow.ApiEventsToDomainLike
import com.github.osxhacker.demo.chassis.domain.ChimneyErrors
import com.github.osxhacker.demo.chassis.domain.entity.ModificationTimes
import com.github.osxhacker.demo.storageFacility.adapter.rest.api
import com.github.osxhacker.demo.storageFacility.domain


/**
 * The '''StorageFacilityApiEventsToDomain''' `object` defines the translation
 * from supported
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.api.StorageFacilityEvent]]s
 * to
 * [[com.github.osxhacker.demo.storageFacility.domain.event.StorageFacilityEvent]]s.
 * It is the complement of the
 * [[com.github.osxhacker.demo.storageFacility.adapter.kafka.arrow.StorageFacilityDomainEventsToApi]]
 * [[https://typelevel.org/cats/typeclasses/arrow.html Arrow]].
 */
object StorageFacilityApiEventsToDomain
	extends ApiEventsToDomainLike[api.StorageFacilityEvent]
		with OriginProperties[api.StorageFacilityEvent]
{
	/// Class Imports
	import chimney.cats._
	import chimney.dsl._
	import shapeless.syntax.inject._
	import domain.event.AllStorageFacilityEvents
	import domain.transformers._


	/// Implicit Conversions
	implicit val caseActivated =
		transform[api.StorageFacilityActivated, AllStorageFacilityEvents] {
			_.intoF[ChimneyErrors, domain.event.StorageFacilityStatusChanged]
				.withFieldComputedF (_.correlationId, mkCorrelationId)
				.withFieldComputedF (_.region, mkRegion)
				.withFieldComputedF (_.fingerprint, mkFingerprint)
				.withFieldConst (_.status, domain.StorageFacilityStatus.Active)
				.transform
				.map (_.inject[AllStorageFacilityEvents])
			}

	implicit val caseClosed =
		transform[api.StorageFacilityClosed, AllStorageFacilityEvents] {
			_.intoF[ChimneyErrors, domain.event.StorageFacilityStatusChanged]
				.withFieldComputedF (_.correlationId, mkCorrelationId)
				.withFieldComputedF (_.region, mkRegion)
				.withFieldComputedF (_.fingerprint, mkFingerprint)
				.withFieldConst (_.status, domain.StorageFacilityStatus.Closed)
				.transform
				.map (_.inject[AllStorageFacilityEvents])
			}

	implicit val caseCreated =
		transform[api.StorageFacilityCreated, AllStorageFacilityEvents] {
			_.intoF[ChimneyErrors, domain.event.StorageFacilityCreated]
				.withFieldComputedF (_.correlationId, mkCorrelationId)
				.withFieldComputedF (_.region, mkRegion)
				.withFieldComputedF (_.fingerprint, mkFingerprint)
				.withFieldComputed (_.timestamps,
					ev => ModificationTimes (
						ev.createdOn.toInstant,
						ev.lastChanged.toInstant
						)
					)
				.transform
				.map (_.inject[AllStorageFacilityEvents])
			}

	implicit val caseDeleted =
		transform[api.StorageFacilityDeleted, AllStorageFacilityEvents] {
			_.intoF[ChimneyErrors, domain.event.StorageFacilityDeleted]
				.withFieldComputedF (_.correlationId, mkCorrelationId)
				.withFieldComputedF (_.region, mkRegion)
				.withFieldComputedF (_.fingerprint, mkFingerprint)
				.transform
				.map (_.inject[AllStorageFacilityEvents])
			}

	implicit val caseProfileChanged =
		transform[api.StorageFacilityProfileChanged, AllStorageFacilityEvents] {
			_.intoF[ChimneyErrors, domain.event.StorageFacilityProfileChanged]
				.withFieldComputedF (_.correlationId, mkCorrelationId)
				.withFieldComputedF (_.region, mkRegion)
				.withFieldComputedF (_.fingerprint, mkFingerprint)
				.transform
				.map (_.inject[AllStorageFacilityEvents])
			}
}

