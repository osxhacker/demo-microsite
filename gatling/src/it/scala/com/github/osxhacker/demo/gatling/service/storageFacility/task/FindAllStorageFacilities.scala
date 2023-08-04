package com.github.osxhacker.demo.gatling.service.storageFacility.task

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Expression
import io.gatling.core.structure.ChainBuilder

import com.github.osxhacker.demo.api.storageFacility._
import com.github.osxhacker.demo.gatling.ServiceEndpoint
import com.github.osxhacker.demo.gatling.service.storageFacility.StorageFacilitySessionKeys


/**
 * The '''FindAllStorageFacilities''' type defines a
 * [[com.github.osxhacker.demo.gatling.service.storageFacility.task.StorageFacilityTask]]
 * responsible for retrieving all currently defined
 * [[com.github.osxhacker.demo.api.storageFacility.StorageFacilities]]
 * associated with the
 * [[com.github.osxhacker.demo.gatling.service.storageFacility.StorageFacilitySessionKeys.OwningCompanyEntry]],
 * conditionally enforcing that the result has at least the `minimumExisting`
 * number of [[com.github.osxhacker.demo.api.storageFacility.StorageFacility]]
 * instances.  On successful completion, the
 * [[com.github.osxhacker.demo.api.storageFacility.StorageFacilities]] resource
 * is accessible via
 * [[com.github.osxhacker.demo.gatling.service.storageFacility.StorageFacilitySessionKeys.StorageFacilitiesEntry]].
 */
final case class FindAllStorageFacilities (
	private val endpoint : ServiceEndpoint
	)
	(implicit override val configuration : GatlingConfiguration)
	extends StorageFacilityTask ()
{
	/// Class Imports
	import StorageFacilitySessionKeys._


	/// Instance Properties
	private val uri : Expression[String] = storageFacilitiesLocation (
		endpoint,
		OwningCompanyEntry.session
		)


	def apply (minimumExisting : Int = 0) : ChainBuilder = exec (
		http ("Find All Storage Facilities by Company").get (uri)
			.addCorrelationId ()
			.logResponse ()
			.check (
				isOk,
				jsonPath ("$.facilities").exists,
				jsonPath ("$.facilities").ofType[Seq[Any]]
					.transform (_.length)
					.gte (minimumExisting)
				)
			.mapTo[StorageFacilities] (StorageFacilitiesEntry)
		)
}

