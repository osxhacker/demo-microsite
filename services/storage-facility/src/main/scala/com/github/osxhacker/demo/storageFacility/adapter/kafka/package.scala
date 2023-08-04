package com.github.osxhacker.demo.storageFacility.adapter

import shapeless.{
	syntax => _,
	_
	}

import com.github.osxhacker.demo.storageFacility.adapter.rest.api


/**
 * The '''kafka''' `package` defines types applicable for interacting with
 * [[https://kafka.apache.org/books-and-papers Apache Kafka]] across specific
 * microservices.
 */
package object kafka
{
	/// Class Types
	/**
	 * The '''AllApiCompanyEventTypes''' type defines the `api` company events
	 * known and supported by the microservice.
	 */
	type AllApiCompanyEventTypes =
		api.CompanyActivated :+:
		api.CompanyCreated :+:
		api.CompanyDeleted :+:
		api.CompanyInactivated :+:
		api.CompanyProfileChanged :+:
		api.CompanySlugChanged :+:
		api.CompanySuspended :+:
		CNil


	/**
	 * The '''AllApiStorageFacilityEventTypes''' type defines the `api`
	 * storage facility events known and supported by the microservice.
	 */
	type AllApiStorageFacilityEventTypes =
		api.StorageFacilityActivated :+:
		api.StorageFacilityClosed :+:
		api.StorageFacilityCreated :+:
		api.StorageFacilityDeleted :+:
		api.StorageFacilityProfileChanged :+:
		CNil


	/**
	 * The '''CompanyKeyType''' type is the Kafka key within each
	 * [[com.github.osxhacker.demo.storageFacility.adapter.kafka.CompanyEventType]].
	 */
	type CompanyKeyType = api.CompanyEvent.IdType


	/**
	 * The '''CompanyEventType''' type is the common ancestor to __all__
	 * Kafka messages (events) relating to
	 * [[com.github.osxhacker.demo.storageFacility.domain.Company]] integration
	 * events.
	 */
	type CompanyEventType = api.CompanyEvent


	/**
	 * The '''StorageFacilityKeyType''' type is the Kafka key within each
	 * [[com.github.osxhacker.demo.storageFacility.adapter.kafka.StorageFacilityEventType]].
	 */
	type StorageFacilityKeyType = api.StorageFacilityEvent.IdType


	/**
	 * The '''StorageFacilityEventType''' type is the common ancestor to __all__
	 * Kafka messages (events) relating to
	 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]]
	 * integration events.
	 */
	type StorageFacilityEventType = api.StorageFacilityEvent
}

