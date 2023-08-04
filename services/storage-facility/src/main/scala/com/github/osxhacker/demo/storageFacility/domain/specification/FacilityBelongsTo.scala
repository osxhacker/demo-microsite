package com.github.osxhacker.demo.storageFacility.domain.specification

import com.github.osxhacker.demo.chassis.domain.Specification
import com.github.osxhacker.demo.storageFacility.domain.{
	Company,
	CompanyReference,
	StorageFacility
	}


/**
 * The '''FacilityBelongsTo''' type is a
 * [[com.github.osxhacker.demo.chassis.domain.Specification]] which
 * `isSatisfiedBy` a
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]] having
 * an `owner` which is identified by the `desired`
 * [[com.github.osxhacker.demo.storageFacility.domain.CompanyReference]].
 */
final case class FacilityBelongsTo (private val desired : CompanyReference)
	extends Specification[StorageFacility]
{
	override def apply (candidate : StorageFacility) : Boolean =
		candidate.belongsTo (desired)


	override def toString () : String =
		"specification: storage facility belongs to"
}


object FacilityBelongsTo
{
	/**
	 * This version of the apply method is provided to support functional-style
	 * creation when given a '''candidate'''
	 * [[com.github.osxhacker.demo.storageFacility.domain.Company]].
	 */
	def apply (company : Company) : FacilityBelongsTo =
		new FacilityBelongsTo (company.toRef ())
}

