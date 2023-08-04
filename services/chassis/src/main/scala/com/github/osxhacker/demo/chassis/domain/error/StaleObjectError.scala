package com.github.osxhacker.demo.chassis.domain.error

import scala.language.postfixOps
import scala.reflect.ClassTag

import cats.Show

import com.github.osxhacker.demo.chassis.domain.entity.{
	Identifier,
	Version
	}


/**
 * The '''StaleObjectError''' type defines the Domain Object Model concept of an
 * attempt to use a domain type which is an older
 * [[com.github.osxhacker.demo.chassis.domain.entity.Version]] of what the
 * system currently has.  If possible, the `latest` instance is provided.
 */
final case class StaleObjectError[DomainT] (
	val id : Identifier[DomainT],
	val version : Version,
	val latest : Option[DomainT] = None,
	val cause : Option[Throwable] = None
	)
	(implicit private val classTag : ClassTag[DomainT])
	extends RuntimeException (
		StaleObjectError.createMessage[DomainT] (id, version),
		cause orNull
		)


object StaleObjectError
{
	/// Class Imports
	import cats.syntax.show._


	private def createMessage[DomainT] (
		id : Identifier[DomainT],
		version : Version
		)
		(implicit classTag : ClassTag[DomainT])
		: String =
		s"stale '${classTag.runtimeClass.getSimpleName}' detected : ${id.show} ${version.show}"


	/// Implicit Conversions
	implicit def staleObjectErrorShow[DomainT]
		: Show[StaleObjectError[DomainT]] =
		Show.show (_.getMessage)
}

