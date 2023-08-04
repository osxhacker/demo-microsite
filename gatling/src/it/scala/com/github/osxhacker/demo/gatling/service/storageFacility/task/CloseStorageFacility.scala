package com.github.osxhacker.demo.gatling.service.storageFacility.task

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Expression
import io.gatling.core.structure.ChainBuilder

import com.github.osxhacker.demo.api.storageFacility._
import com.github.osxhacker.demo.gatling.ServiceEndpoint
import com.github.osxhacker.demo.gatling.service.storageFacility.StorageFacilitySessionKeys


/**
 * The '''CloseStorageFacility''' type defines the
 * [[com.github.osxhacker.demo.gatling.service.storageFacility.task.StorageFacilityTask]]
 * responsible for modifying the status of a
 * [[com.github.osxhacker.demo.api.storageFacility.StorageFacility]] in
 * [[com.github.osxhacker.demo.gatling.service.storageFacility.StorageFacilitySessionKeys.TargetStorageFacilityEntry]]
 * to be
 * [[com.github.osxhacker.demo.api.storageFacility.StorageFacilityStatus.Closed]].
 * On successful completion, the [[io.gatling.core.session.Session]] will have
 * the newly altered
 * [[com.github.osxhacker.demo.api.storageFacility.StorageFacility]] in
 * [[com.github.osxhacker.demo.gatling.service.storageFacility.StorageFacilitySessionKeys.TargetStorageFacilityEntry]].
 */
final case class CloseStorageFacility (
	private val endpoint : ServiceEndpoint
	)
	(implicit override val configuration: GatlingConfiguration)
	extends StorageFacilityTask ()
{
	/// Class Imports
	import StorageFacilitySessionKeys._
	import io.gatling.commons.validation._


	/// Instance Properties
	private val closeAction : Expression[String] =
		TargetStorageFacilityEntry.session
			.to (hrefFor (storageFacility.close))
			.some
			.headOption (_)
			.toValidation ("unable to resolve the 'close' action")
			.map (endpoint / _)

	private val facilityVersion : Expression[String] =
		TargetStorageFacilityEntry.session
			.headOption (_)
			.toValidation ("target facility does not exist")
			.map {
				company =>
					VersionOnly (company.version).asJsonString ()
				}

	def apply () : ChainBuilder = exec (
		http ("Close Storage Facility").post (closeAction)
			.addCorrelationId ()
			.body (StringBody (facilityVersion))
			.check (isOk)
			.logResponse ()
			.mapTo[StorageFacility] (TargetStorageFacilityEntry)
		)
		.exitHereIfFailed
}

