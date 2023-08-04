package com.github.osxhacker.demo.company.domain.specification

import com.github.osxhacker.demo.chassis.domain.Specification
import com.github.osxhacker.demo.company.domain.Company


/**
 * The '''CompanyNameIs''' type is a
 * [[com.github.osxhacker.demo.chassis.domain.Specification]] which
 * `isSatisfiedBy` a [[com.github.osxhacker.demo.company.domain.Company]] having
 * its `name` the same as the '''desired''' one given during construction.
 */
final case class CompanyNameIs (private val desired : Company.Name)
	extends Specification[Company]
{
	/// Class Imports
	import cats.syntax.eq._


	override def apply (candidate : Company) : Boolean =
		Company.name.get (candidate).value === desired.value


	override def toString () : String = "specification: company name is"
}


object CompanyNameIs
{
	/**
	 * This version of the apply method is provided to allow functional-style
	 * creation when the '''company''' is known.
	 */
	def apply (company : Company) : CompanyNameIs =
		new CompanyNameIs (Company.name.get (company))
}

