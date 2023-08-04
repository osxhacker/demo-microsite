package com.github.osxhacker.demo.storageFacility.domain.specification

import com.github.osxhacker.demo.chassis.domain.Specification
import com.github.osxhacker.demo.chassis.domain.event.Region
import com.github.osxhacker.demo.storageFacility.domain.StorageFacility


/**
 * The '''FacilityIsReadOnly''' type is a
 * [[com.github.osxhacker.demo.chassis.domain.Specification]] which
 * `isSatisfiedBy` __any one__ of the following conditions being `true`:
 *
 *   - The [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]]
 *     having a `primary`
 *     [[com.github.osxhacker.demo.chassis.domain.event.Region]] which is
 *     different than the one given in the [[scala.Tuple2]].
 *
 *   - The [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]]
 *     `owner` does not satisfy the
 *     [[com.github.osxhacker.demo.storageFacility.domain.specification.CompanyIsActive]]
 *     [[com.github.osxhacker.demo.chassis.domain.Specification]].
 */
final case class FacilityIsReadOnly ()
	extends Specification[(StorageFacility, Region)]
{
	/// Class Imports
	import StorageFacility.owner
	import cats.syntax.eq._


	/// Instance Properties
	private val companyIsActive = CompanyIsActive (owner)


	override def apply (params : (StorageFacility, Region)) : Boolean =
		(
			companyIsActive (params._1) &&
			params._1
				.definedIn (params._2)
		) === false


	override def toString () : String =
		"specification: storage facility is read-only"
}

