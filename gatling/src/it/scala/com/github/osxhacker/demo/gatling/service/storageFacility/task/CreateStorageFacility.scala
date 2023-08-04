package com.github.osxhacker.demo.gatling.service.storageFacility.task

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.structure.ChainBuilder

import com.github.osxhacker.demo.api.storageFacility._
import com.github.osxhacker.demo.gatling.ServiceEndpoint
import com.github.osxhacker.demo.gatling.service.storageFacility.StorageFacilitySessionKeys


/**
 * The '''CreateStorageFacility''' type defines the
 * [[com.github.osxhacker.demo.gatling.service.storageFacility.task.StorageFacilityTask]]
 * responsible for creating a new
 * [[com.github.osxhacker.demo.api.storageFacility.StorageFacility]].  On
 * successful completion, the [[io.gatling.core.session.Session]] will have the
 * newly created
 * [[com.github.osxhacker.demo.api.storageFacility.StorageFacility]] in
 * [[com.github.osxhacker.demo.gatling.service.storageFacility.StorageFacilitySessionKeys.TargetStorageFacilityEntry]].
 */
final case class CreateStorageFacility (private val endpoint : ServiceEndpoint)
	(implicit override val configuration : GatlingConfiguration)
	extends StorageFacilityTask ()
{
	/// Class Imports
	import StorageFacilitySessionKeys._
	import io.gatling.commons.validation._


	/// Instance Properties
	private val locationEntry = "tmp-location"
	private val uri = storageFacilitiesLocation (
		endpoint,
		OwningCompanyEntry.session
		)


	def apply (facility : => NewStorageFacility) : ChainBuilder = exec (
		http ("Create New Facility").put (uri)
			.addCorrelationId ()
			.body (StringBody (_ => facility.asJsonString ().success))
			.logResponse ()
			.check (
				isCreated,
				header (HttpHeaderNames.Location).exists
					.saveAs (locationEntry)
				)
		)
		.exitHereIfFailed
		.exec (
			http ("Retrieve Created Storage Facility").get {
				_ (locationEntry).validate[String]
					.map (endpoint / _)
				}
				.addCorrelationId ()
				.logResponse ()
				.check (isOk)
				.mapTo[StorageFacility] (TargetStorageFacilityEntry)
		)
		.exec (_.remove (locationEntry))
		.exitHereIfFailed
		.exec (logSession ("After 'create facility':"))


	def shouldFailWith (facility : NewStorageFacility) : ChainBuilder =
		exec (
			http ("Create Invalid Facility").put (uri)
				.addCorrelationId ()
				.body (facility)
				.logResponse ()
				.check (isClientError)
			)
			.exitHereIfFailed
}

