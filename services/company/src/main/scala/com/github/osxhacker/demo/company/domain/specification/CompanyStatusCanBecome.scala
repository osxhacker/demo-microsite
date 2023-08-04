package com.github.osxhacker.demo.company.domain.specification

import com.github.osxhacker.demo.chassis.domain.Specification
import com.github.osxhacker.demo.company.domain.{
	Company,
	CompanyStatus
	}


/**
 * The '''CompanyStatusCanBecome''' type is a
 * [[com.github.osxhacker.demo.chassis.domain.Specification]] which
 * `isSatisfiedBy` a [[com.github.osxhacker.demo.company.domain.Company]] having
 * its `status` capable of becoming the '''desired''' one given during
 * construction.
 */
final case class CompanyStatusCanBecome (private val desired : CompanyStatus)
	extends Specification[Company]
{
	override def apply (company : Company) : Boolean =
		Company.status
			.get (company)
			.canBecome (desired)


	override def toString () : String =
		"specification: company status can become"
}

