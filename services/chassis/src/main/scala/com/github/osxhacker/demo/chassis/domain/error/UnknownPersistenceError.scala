package com.github.osxhacker.demo.chassis.domain.error

import scala.language.postfixOps

import cats.Show


/**
 * The '''UnknownPersistenceError''' defines the Domain Object Model
 * representation of an error when interacting with a persistent-store.  This
 * is emitted when no other suitable categorization exists.
 */
final case class UnknownPersistenceError (
	val message : String,
	val cause : Option[Throwable] = None
	)
	extends RuntimeException (
		message,
		cause orNull
		)


object UnknownPersistenceError
{
	/// Implicit Conversions
	implicit val unknownPersistenceErrorShow : Show[UnknownPersistenceError] =
		Show.show (_.getMessage)
}

