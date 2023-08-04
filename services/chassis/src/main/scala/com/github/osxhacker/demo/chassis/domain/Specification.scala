package com.github.osxhacker.demo.chassis.domain

import scala.annotation.unused
import scala.language.implicitConversions

import cats.Show


/**
 * The '''Specification''' type is a contract for identifying Domain Object
 * Model types which satisfy the requirements of concrete implementations.
 * They can be viewed as
 * [[http://refactoring.com/catalog/replaceMethodWithMethodObject.html Method Objects]]
 * suitable for use in `filter`ing candidate instances.
 */
trait Specification[A]
	extends (A => Boolean)
{
	override def toString () : String = "<specification>"


	/**
	 * The isSatisfiedBy method is a named alias for the `apply` method.
	 */
	final def isSatisfiedBy (candidate : A) : Boolean = apply (candidate)
}


object Specification
{
	/// Class Types
	implicit class ComposeSpecification[A] (val lhs : Specification[A])
		extends AnyVal
	{
		/**
		 * The &amp;&amp; method composes two instances of '''Specification''',
		 * with this instance first deciding satisfaction ''and then'' the rhs.
		 */
		def && (rhs : Specification[A]): Specification[A] =
			AndSpecification (lhs, rhs)


		/**
		 * The || method composes two instances of '''Specification''', with
		 * this instance first deciding satisfaction ''or else'' the rhs.
		 */
		def || (rhs : Specification[A]): Specification[A] =
			OrSpecification (lhs, rhs)


		/**
		 * The unary_! method allows logical negation of a '''Specification'''.
		 */
		def unary_! : Specification[A] = NotSpecification (lhs)
	}


	/**
	 * The apply method allows for creation of a '''Specification''' using an
	 * anonymous '''inline''' method.  Care must be taken when using this as
	 * transmitting a '''Specification''' made this way over a network will
	 * likely fail.
	 */
	def apply[T] (inline : T => Boolean) : Specification[T] =
		new Specification[T] {
			override def toString () : String = "<inline>"


			override def apply (candidate : T) : Boolean = inline (candidate)
		}


	/**
	 * The const method allows for creation of a '''Specification''' resulting
	 * in a constant '''value'''.  Care must be taken when using this as
	 * transmitting a '''Specification''' made this way over a network will
	 * likely fail.
	 */
	def const[T] (value : Boolean) : Specification[T] =
		new Specification[T]
		{
			override def toString () : String = s"<$value>"


			override def apply (@unused candidate : T) : Boolean = value
		}


	/// Implicit Conversions
	implicit def specificationFromFunctor[A] (f : A => Boolean)
		: Specification[A] =
		new Specification[A] {
			override def apply (a : A): Boolean = f (a)
			}


	implicit def specificationShow[A] : Show[Specification[A]] =
		Show.fromToString
}


final case class AndSpecification[A] (
	private val lhs : Specification[A],
	private val rhs : Specification[A]
)
	extends Specification[A]
{
	/// Instance Properties
	override def toString () : String = s"($lhs) && ($rhs)"


	override def apply (candidate : A) : Boolean =
		lhs (candidate) && rhs (candidate)
}


final case class NotSpecification[A] (private val spec : Specification[A])
	extends Specification[A]
{
	/// Instance Properties
	override def toString () : String = s"!($spec)"


	override def apply (candidate : A) : Boolean = !spec (candidate)
}


final case class OrSpecification[A] (
	private val lhs : Specification[A],
	private val rhs : Specification[A]
)
	extends Specification[A]
{
	/// Instance Properties
	override def toString () : String = s"($lhs) || ($rhs)"


	override def apply (candidate : A) : Boolean =
		lhs (candidate) || rhs (candidate)
}
