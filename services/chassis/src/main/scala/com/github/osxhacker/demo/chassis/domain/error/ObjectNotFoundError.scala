package com.github.osxhacker.demo.chassis.domain.error

import scala.language.postfixOps
import scala.reflect.ClassTag

import cats.Show

import com.github.osxhacker.demo.chassis.domain.entity.Identifier


/**
 * The '''ObjectNotFoundError''' type defines the Domain Object Model concept of
 * an attempt to access a domain type which is cannot be resolved in the
 * persistent store by its `id` alone.
 */
final case class ObjectNotFoundError[DomainT] (
	val id : Identifier[DomainT],
	val cause : Option[Throwable] = None
	)
	(implicit private val classTag : ClassTag[DomainT])
	extends RuntimeException (
		ObjectNotFoundError.createMessage[DomainT] (id),
		cause orNull
		)


object ObjectNotFoundError
{
	/// Class Imports
	import cats.syntax.show._


	private def createMessage[DomainT] (id : Identifier[DomainT])
		(implicit classTag : ClassTag[DomainT])
		: String =
		s"'${classTag.runtimeClass.getSimpleName}' not found : ${id.show}"


	/// Implicit Conversions
	implicit def objectNotFoundErrorShow[DomainT]
		: Show[ObjectNotFoundError[DomainT]] =
		Show.show (_.getMessage)
}

