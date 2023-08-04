package com.github.osxhacker.demo.company.domain.specification

import com.github.osxhacker.demo.chassis.domain.{
	Slug,
	Specification
	}

import com.github.osxhacker.demo.company.domain.Company


/**
 * The '''CompanySlugIs''' type is a
 * [[com.github.osxhacker.demo.chassis.domain.Specification]] which
 * `isSatisfiedBy` a [[com.github.osxhacker.demo.company.domain.Company]] having
 * its `slug` the same as the '''desired''' one given during construction.
 */
final case class CompanySlugIs (private val desired : Slug)
	extends Specification[Company]
{
	/// Class Imports
	import cats.syntax.eq._


	override def apply (candidate : Company) : Boolean =
		Company.slug.get (candidate) === desired


	override def toString () : String = "specification: company slug is"
}


object CompanySlugIs
{
	/**
	 * This version of the apply method is provided to allow functional-style
	 * creation when the '''company''' is known.
	 */
	def apply (company : Company) : CompanySlugIs =
		new CompanySlugIs (Company.slug.get (company))
}

