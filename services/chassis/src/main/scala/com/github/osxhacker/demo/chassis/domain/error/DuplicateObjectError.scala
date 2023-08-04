package com.github.osxhacker.demo.chassis.domain.error

import scala.language.postfixOps
import scala.reflect.ClassTag

import cats.Show

import com.github.osxhacker.demo.chassis.domain.entity.Identifier


/**
 * The '''DuplicateObjectError''' type defines the Domain Object Model concept
 * of an attempt to persist a domain type which already exists within the
 * persistent store by its `id` alone.
 */
final case class DuplicateObjectError[DomainT] (
	val id : Identifier[DomainT],
	val cause : Option[Throwable] = None
	)
	(implicit private val classTag : ClassTag[DomainT])
	extends RuntimeException (
		DuplicateObjectError.createMessage[DomainT] (id),
		cause orNull
	)


object DuplicateObjectError
{
	/// Class Imports
	import cats.syntax.show._


	private def createMessage[DomainT] (id : Identifier[DomainT])
		(implicit classTag : ClassTag[DomainT])
		: String =
		s"duplicate '${classTag.runtimeClass.getSimpleName}' : ${id.show}"


	/// Implicit Conversions
	implicit def duplicateObjectErrorShow[DomainT]
		: Show[DuplicateObjectError[DomainT]] =
		Show.show (_.getMessage)
}

