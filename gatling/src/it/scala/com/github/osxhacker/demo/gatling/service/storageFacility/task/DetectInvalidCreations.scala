package com.github.osxhacker.demo.gatling.service.storageFacility.task

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.structure.ChainBuilder

import com.github.osxhacker.demo.api.storageFacility._


/**
 * The '''DetectInvalidCreations''' type defines the
 * [[com.github.osxhacker.demo.gatling.service.storageFacility.task.StorageFacilityTask]]
 * responsible for attempting to create invalid
 * [[com.github.osxhacker.demo.api.storageFacility.StorageFacility]] instances.
 *
 * This
 * [[com.github.osxhacker.demo.gatling.service.storageFacility.task.StorageFacilityTask]]
 * uses a table-driven technique for creating invalid
 * [[com.github.osxhacker.demo.api.storageFacility.StorageFacility]] instances,
 * each intended to check a specific erroneous condition.
 */
final case class DetectInvalidCreations (
	private val create : CreateStorageFacility
	)
	(implicit override val configuration : GatlingConfiguration)
	extends StorageFacilityTask ()
{
	/// Class Imports
	import StorageFacilityStatus.Active


	/// Class Types
	private type ArgumentsType = (
		String,
		StorageFacilityStatus,
		String,
		String,
		String,
		BigDecimal,
		BigDecimal
		)


	/// Instance Properties
	private val mkNewStorageFacility = (NewStorageFacility.apply _).tupled

	private val available = BigDecimal (9_000)
	private val capacity = BigDecimal (10_000)
	private val invalidParameters : Seq[ArgumentsType] =
		("  invalid name  ", Active, "Chicago", "IL", "60604", capacity, available) ::
		("invalid city", Active, "", "IL", "60604", capacity, available) ::
		("invalid state", Active, "Chicago", "bad!", "60604", capacity, available) ::
		("invalid zip", Active, "Chicago", "IL", "abcde", capacity, available) ::
		("invalid capacity", Active, "Chicago", "IL", "60604", -capacity, available) ::
		("invalid available", Active, "Chicago", "IL", "60604", capacity, -available) ::
		("available swapped", Active, "Chicago", "IL", "60604", available, capacity) ::
		("  multiple bad  ", Active, "", "", "", available, capacity) ::
		Nil


	def apply () : ChainBuilder = exitBlockOnFail (
		exec (
			invalidParameters.map (mkNewStorageFacility)
				.map (create.shouldFailWith)
			)
		)
}

