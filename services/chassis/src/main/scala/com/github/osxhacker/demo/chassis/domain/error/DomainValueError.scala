package com.github.osxhacker.demo.chassis.domain.error

import scala.language.postfixOps

import cats.Show


/***
 * The '''DomainValueError''' type defines the Domain Object Model concept of
 * indicating when an attempt to use a value is disallowed due to domain rules.
 */
final case class DomainValueError (val message : String)
	extends RuntimeException (message)


object DomainValueError
{
	/// Implicit Conversions
	implicit val domainValueErrorShow : Show[DomainValueError] =
		Show.show (_.getMessage)
}

