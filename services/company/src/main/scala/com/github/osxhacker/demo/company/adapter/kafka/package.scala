package com.github.osxhacker.demo.company.adapter

import shapeless.{
	syntax => _,
	_
	}

import com.github.osxhacker.demo.company.adapter.rest.api


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
	 * The '''CompanyKeyType''' type is the Kafka key within each
	 * [[com.github.osxhacker.demo.company.adapter.kafka.CompanyEventType]].
	 */
	type CompanyKeyType = api.CompanyEvent.IdType


	/**
	 * The '''CompanyEventType''' type is the common ancestor to __all__
	 * Kafka messages (events) relating to
	 * [[com.github.osxhacker.demo.company.domain.Company]] integration events.
	 */
	type CompanyEventType = api.CompanyEvent
}
