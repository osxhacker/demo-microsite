package com.github.osxhacker.demo.storageFacility.domain.specification


import com.github.osxhacker.demo.chassis.domain.Specification
import com.github.osxhacker.demo.storageFacility.domain.{
	StorageFacility,
	StorageFacilityStatus
	}


/**
 * The '''FacilityIsActive''' type is a
 * [[com.github.osxhacker.demo.chassis.domain.Specification]] which
 * `isSatisfiedBy` a
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]] having
 * a `status` of
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacilityStatus.Active]]
 * __and__ the
 * [[com.github.osxhacker.demo.storageFacility.domain.specification.CompanyIsActive]].
 */
final case class FacilityIsActive ()
	extends Specification[StorageFacility]
{
	/// Class Imports
	import StorageFacility.owner
	import StorageFacilityStatus.Active


	/// Instance Properties
	private val isActive = CompanyIsActive (owner) && FacilityStatusIs (Active)


	override def apply (candidate : StorageFacility) : Boolean =
		isActive (candidate)


	override def toString () : String =
		"specification: storage facility is active"
}

