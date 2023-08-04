package com.github.osxhacker.demo.storageFacility.domain.specification

import com.github.osxhacker.demo.chassis.domain.Specification
import com.github.osxhacker.demo.storageFacility.domain.{
	StorageFacility,
	StorageFacilityStatus
	}


/**
 * The '''FacilityStatusCanBecome ''' type is a
 * [[com.github.osxhacker.demo.chassis.domain.Specification]] which
 * `isSatisfiedBy` a
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]] having
 * a `status` which can transition to the '''desired''' one given.
 */
final case class FacilityStatusCanBecome (
	private val desired : StorageFacilityStatus
	)
	extends Specification[StorageFacility]
{
	override def apply (storageFacility : StorageFacility) : Boolean =
		StorageFacility.status
			.get (storageFacility)
			.canBecome (desired)


	override def toString (): String =
		"specification: storage facility can become"
}

