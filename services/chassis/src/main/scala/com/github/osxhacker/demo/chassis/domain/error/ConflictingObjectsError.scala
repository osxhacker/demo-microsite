package com.github.osxhacker.demo.chassis.domain.error

import scala.language.postfixOps
import scala.reflect.ClassTag

import cats.Show


/**
 * The '''ConflictingObjectsError''' type defines the Domain Object Model
 * concept of an attempt to use a ''DomainT'' instance which has an existing
 * representation that has differences which cannot be resolved.
 */
final case class ConflictingObjectsError[DomainT] (
	val message : String,
	val cause : Option[Throwable] = None
	)
	(implicit private val classTag : ClassTag[DomainT])
	extends RuntimeException (
		ConflictingObjectsError.createMessage[DomainT] (message),
		cause orNull
		)


object ConflictingObjectsError
{
	/// Class Imports
	import cats.syntax.show._


	private def createMessage[DomainT] (message : String)
		(implicit classTag : ClassTag[DomainT])
		: String =
		new StringBuilder ()
			.append ("conflicting '")
			.append (classTag.runtimeClass.getSimpleName)
			.append ("' detected : ")
			.append (message)
			.toString ()


	/// Implicit Conversions
	implicit def conflictingObjectsErrorShow[DomainT]
		: Show[ConflictingObjectsError[DomainT]] =
		Show.show (_.getMessage)
}

