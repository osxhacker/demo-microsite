package com.github.osxhacker.demo.chassis.domain.error

import scala.language.postfixOps

import cats.Show


/***
 * The '''LogicError''' type defines the Domain Object Model concept of
 * indicating when a condition has been detected which "should not exist."
 * For example, when violating constraints defined in a persistent store or
 * expecting a value within a collection when there are none.
 */
final case class LogicError (
	val message : String,
	val cause : Option[Throwable] = None
	)
	extends RuntimeException (
		message,
		cause orNull
		)


object LogicError
{
	/// Implicit Conversions
	implicit val unknownPersistenceErrorShow : Show[LogicError] =
		Show.show (_.getMessage)
}

