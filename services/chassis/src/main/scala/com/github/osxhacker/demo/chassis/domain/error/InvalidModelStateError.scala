package com.github.osxhacker.demo.chassis.domain.error

import scala.language.postfixOps
import scala.reflect.ClassTag

import cats.Show
import com.github.osxhacker.demo.chassis.domain.entity.{
	Identifier,
	Version
	}


/**
 * The '''InvalidModelStateError''' type defines the Domain Object Model concept
 * of an attempt to use a domain type in a manner not supported by the rules
 * defined for the ''DomainT''.
 */
final case class InvalidModelStateError[DomainT] (
	val id : Identifier[DomainT],
	val version : Version,
	val message : String,
	val cause : Option[Throwable] = None
	)
	(implicit private val classTag : ClassTag[DomainT])
	extends RuntimeException (
		InvalidModelStateError.createMessage[DomainT] (id, version, message),
		cause orNull
		)


object InvalidModelStateError
{
	/// Class Imports
	import cats.syntax.show._


	/**
	 * This version of the apply method supports functional-style creation for
	 * when ''DomainT'' does not have nor support a
	 * [[com.github.osxhacker.demo.chassis.domain.entity.Version]] and there is
	 * __not__ a '''cause'''.
	 */
	def apply[DomainT] (id : Identifier[DomainT], message : String)
		(implicit classTag : ClassTag[DomainT])
		: InvalidModelStateError[DomainT] =
		new InvalidModelStateError[DomainT] (
			id,
			Version.initial,
			message,
			None
			)


	/**
	 * This version of the apply method supports functional-style creation for
	 * when ''DomainT'' does not have nor support a
	 * [[com.github.osxhacker.demo.chassis.domain.entity.Version]] and there
	 * __is__ a '''cause'''.
	 */
	def apply[DomainT] (
		id : Identifier[DomainT],
		message : String,
		cause : Throwable
		)
		(implicit classTag : ClassTag[DomainT])
		: InvalidModelStateError[DomainT] =
		new InvalidModelStateError[DomainT] (
			id,
			Version.initial,
			message,
			Option (cause)
			)


	private def createMessage[DomainT] (
		id : Identifier[DomainT],
		version : Version,
		message : String
		)
		(implicit classTag : ClassTag[DomainT])
		: String =
		s"'${classTag.runtimeClass.getSimpleName}' $message : ${id.show} ${version.show}"


	/// Implicit Conversions
	implicit def invalidModelStateErrorShow[DomainT]
		: Show[InvalidModelStateError[DomainT]] =
		Show.show (_.getMessage)
}

