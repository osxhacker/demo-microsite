package com.github.osxhacker.demo.storageFacility.domain.specification

import monocle.{
	Getter,
	Iso
	}

import com.github.osxhacker.demo.chassis.domain.Specification
import com.github.osxhacker.demo.storageFacility.domain.{
	Company,
	CompanyStatus
	}


/**
 * The '''CompanyIsActive''' type is a
 * [[com.github.osxhacker.demo.chassis.domain.Specification]] which
 * `isSatisfiedBy` a
 * [[com.github.osxhacker.demo.storageFacility.domain.Company]] having
 * a `status` of
 * [[com.github.osxhacker.demo.storageFacility.domain.CompanyStatus.Active]].
 *
 * Since a [[com.github.osxhacker.demo.storageFacility.domain.Company]] can
 * exist independent of a
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]], such as
 * when it is the tenant, this
 * [[com.github.osxhacker.demo.chassis.domain.Specification]] abstracts this by
 * using a [[monocle.Getter]] to resolve the
 * [[com.github.osxhacker.demo.storageFacility.domain.Company]].
 */
final case class CompanyIsActive[DomainT] (
	private val company : Getter[DomainT, Company]
	)
	extends Specification[DomainT]
{
	/// Class Imports
	import CompanyStatus.Active
	import cats.syntax.eq._


	override def apply (candidate : DomainT) : Boolean =
		company.andThen (Company.status)
			.get (candidate) === Active


	override def toString () : String = "specification: company is active"
}


object CompanyIsActive
{
	/**
	 * This version of the apply method is provided to support functional-style
	 * creation when the '''CompanyIsActive''' instance is used with an
	 * [[https://en.wikipedia.org/wiki/Morphism#Identity identity morphism]].
	 */
	def apply () : CompanyIsActive[Company] =
		new CompanyIsActive[Company] (Iso.id)
}

