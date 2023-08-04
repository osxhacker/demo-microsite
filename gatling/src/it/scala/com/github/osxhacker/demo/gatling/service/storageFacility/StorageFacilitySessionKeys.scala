package com.github.osxhacker.demo.gatling.service.storageFacility

import scala.language.{
	implicitConversions,
	postfixOps
	}

import enumeratum._

import com.github.osxhacker.demo.api
import com.github.osxhacker.demo.gatling.SessionKey


/**
 * The '''StorageFacilitySessionKeys''' type defines __all__ known
 * [[io.gatling.core.session.Session]] keys available when interacting with
 * the Storage Facility service.
 */
sealed trait StorageFacilitySessionKeys
	extends SessionKey


object StorageFacilitySessionKeys
	extends Enum[StorageFacilitySessionKeys]
{
	/// Class Types
	case object OwningCompanyEntry
		extends SessionKey.Definition[
			StorageFacilitySessionKeys,
			api.company.Company
			]
			with StorageFacilitySessionKeys


	case object StorageFacilitiesEntry
		extends SessionKey.Definition[
			StorageFacilitySessionKeys,
			api.storageFacility.StorageFacilities
			]
			with StorageFacilitySessionKeys


	case object TargetStorageFacilityEntry
		extends SessionKey.Definition[
			StorageFacilitySessionKeys,
			api.storageFacility.StorageFacility
			]
			with StorageFacilitySessionKeys


	/// Instance Properties
	override val values : IndexedSeq[StorageFacilitySessionKeys] = findValues


	/// Implicit Conversions
	implicit def companySessionKeysToString[A <: StorageFacilitySessionKeys] (
		instance : A
		)
		: String =
		instance.entryName
}

