package com.github.osxhacker.demo.company.domain.specification

import com.github.osxhacker.demo.chassis.domain.Specification
import com.github.osxhacker.demo.company.domain.{
	Company,
	CompanyStatus
	}


/**
 * The '''CompanyStatusIs''' type is a
 * [[com.github.osxhacker.demo.chassis.domain.Specification]] which
 * `isSatisfiedBy` a [[com.github.osxhacker.demo.company.domain.Company]] having
 * the '''desired''' `status`.
 */
final case class CompanyStatusIs (private val desired : CompanyStatus)
	extends Specification[Company]
{
	/// Class Imports
	import cats.syntax.eq._


	override def apply (company : Company) : Boolean =
		Company.status
			.get (company) === desired


	override def toString () : String = "specification: company status is"
}

