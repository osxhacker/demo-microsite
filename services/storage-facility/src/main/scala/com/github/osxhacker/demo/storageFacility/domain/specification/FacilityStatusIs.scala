package com.github.osxhacker.demo.storageFacility.domain.specification

import com.github.osxhacker.demo.chassis.domain.Specification
import com.github.osxhacker.demo.storageFacility.domain.{
	StorageFacility,
	StorageFacilityStatus
	}


/**
 * The '''FacilityStatusIs''' type is a
 * [[com.github.osxhacker.demo.chassis.domain.Specification]] which
 * `isSatisfiedBy` a
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]] having
 * the '''desired''' `status`.
 */
final case class FacilityStatusIs (private val desired : StorageFacilityStatus)
	extends Specification[StorageFacility]
{
	/// Class Imports
	import cats.syntax.eq._


	override def apply (storageFacility : StorageFacility) : Boolean =
		StorageFacility.status
			.get (storageFacility) === desired


	override def toString () : String =
		"specification: storage facility status is"
}

