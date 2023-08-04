package com.github.osxhacker.demo.storageFacility.domain.specification

import eu.timepit.refined

import com.github.osxhacker.demo.chassis.domain.Specification
import com.github.osxhacker.demo.storageFacility.domain.StorageFacility


/**
 * The '''FacilityNameIs''' type is a
 * [[com.github.osxhacker.demo.chassis.domain.Specification]] which
 * `isSatisfiedBy` a
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]] having
 * its `name` the same as the '''desired''' one given during construction.
 */
final case class FacilityNameIs (private val desired : StorageFacility.Name)
	extends Specification[StorageFacility]
{
	/// Class Imports
	import cats.syntax.eq._
	import refined.cats._


	override def apply (candidate : StorageFacility) : Boolean =
		desired === StorageFacility.name.get (candidate)


	override def toString () : String =
		"specification: storage facility name is"
}


object FacilityNameIs
{
	/**
	 * This version of the apply method is provided to allow functional-style
	 * creation when the '''facility''' is known.
	 */
	def apply (facility : StorageFacility) : FacilityNameIs =
		new FacilityNameIs (StorageFacility.name.get (facility))
}

