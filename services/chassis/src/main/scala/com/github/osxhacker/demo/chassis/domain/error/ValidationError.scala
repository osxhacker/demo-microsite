package com.github.osxhacker.demo.chassis.domain.error

import scala.reflect.ClassTag

import cats.{
	Semigroup,
	Show
	}

import cats.data.NonEmptyChain


/***
 * The '''ValidationError''' type defines the Domain Object Model concept of
 * reporting problems with information given to a workflow which violates its
 * rules.
 */
final case class ValidationError[DomainT] (
	private val errors : NonEmptyChain[String]
	)
	(implicit private val classTag : ClassTag[DomainT])
	extends RuntimeException (
		ValidationError.createMessage[DomainT] (errors)
		)


object ValidationError
{
	/// Class Imports
	import cats.syntax.foldable._
	import cats.syntax.semigroup._


	/**
	 * This version of the apply method is provided to support functional-style
	 * creation of a '''ValidationError''' having one '''error'''.
	 */
	def apply[DomainT] (error : String)
		(implicit classTag : ClassTag[DomainT])
		: ValidationError[DomainT] =
		new ValidationError[DomainT] (NonEmptyChain.one (error))


	private def createMessage[DomainT] (errors : NonEmptyChain[String])
		(implicit classTag : ClassTag[DomainT])
		: String =
		new StringBuilder ()
			.append ("validation failed for '")
			.append (classTag.runtimeClass.getSimpleName)
			.append ("' : ")
			.append (errors.mkString_ (", "))
			.toString ()


	/// Implicit Conversions
	implicit def validationErrorShow[DomainT] : Show[ValidationError[DomainT]] =
		Show.show (_.getMessage)


	implicit def validationErrorSemigroup[DomainT]
		: Semigroup[ValidationError[DomainT]] =
		new Semigroup[ValidationError[DomainT]] {
			override def combine (
				a : ValidationError[DomainT],
				b : ValidationError[DomainT]
				)
				: ValidationError[DomainT] =
				ValidationError[DomainT] (a.errors |+| b.errors) (a.classTag)
			}
}

