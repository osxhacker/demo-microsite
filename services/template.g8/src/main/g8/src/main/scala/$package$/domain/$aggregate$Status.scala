package $package$.domain

import scala.annotation.unused

import com.softwaremill.diffx.Diff
import enumeratum._
import shapeless.{
	syntax => _,
	_
	}

import shapeless.PolyDefns.Case


/**
 * The '''$aggregate$Status''' type defines the Domain Object Model
 * representation of __all__ discrete
 * [[$package$.domain.$aggregate$]] status
 * indicators.  Note that some transitions may be disallowed due to domain
 * rules.
 */
sealed trait $aggregate$Status
	extends EnumEntry
{
	/// Class Imports
	import cats.syntax.eq._


	/**
	 * The canBecome method determines if '''this''' [[enumeratum.EnumEntry]]
	 * allows a transition to the '''candidate''' '''$aggregate$Status'''.
	 * Transitioning to the same '''$aggregate$Status''' is always allowed.
	 */
	final def canBecome (desired : $aggregate$Status) : Boolean =
		this === desired || allow (desired)


	/**
	 * The pfmap method defines a polymorphic functor `map`ping of all
	 * '''$aggregate$Status'''es to a common result type ''R''.  In essence,
	 * it is a functional programming form of the
	 * [[https://en.wikipedia.org/wiki/Visitor_pattern visitor pattern]] with
	 * compile-time verification.  Where this differs from a typical OOP
	 * [[https://en.wikipedia.org/wiki/Visitor_pattern visitor pattern]]
	 * implementation is by providing an arbitrary ''A'' type for context.
	 * For example:
	 *
	 * {{{
	 *     object UseStatus
	 *     {
	 *         implicit val caseActive : Case.Aux[Active.type, A, R] = ???
	 *         implicit val caseInactive : Case.Aux[Inactive.type, A, R] = ???
	 *     }
	 *
	 *     ...
	 *
	 *     val context : A = ???
	 *     val result : R = a$aggregate$.status
	 *         .pfmap[UseStatus, A, R] (context)
	 * }}}
	 *
	 * Note the explicit type annotation provided for each `implicit val` in
	 * the example.  This is needed when using the [[shapeless.Poly2]] `at`
	 * method, as it returns a [[shapeless.Poly2.CaseBuilder]] and not a
	 * [[shapeless.PolyDefns.Case.Aux]] directly.
	 */
	def pfmap[PolyT <: Poly2, A, R] (a : A)
		(
			implicit
			active : Case.Aux[
				PolyT,
				$aggregate$Status.Active.type :: A :: HNil,
				R
				],

			inactive : Case.Aux[
				PolyT,
				$aggregate$Status.Inactive.type :: A :: HNil,
				R
				]
		)
		: R


	/**
	 * The allow abstract method is a model of the TEMPLATE pattern and is only
	 * invoked when '''this''' instance is __not__ the '''desired''' one given
	 * to `canBecome` in order to complete the decision process.
	 */
	protected def allow (desired : $aggregate$Status) : Boolean
}


object $aggregate$Status
	extends Enum[$aggregate$Status]
		with CatsEnum[$aggregate$Status]
{
	/// Class Imports
	import cats.syntax.eq._


	/// Class Types
	case object Active
		extends $aggregate$Status
	{
		override def pfmap[PolyT <: Poly2, A, R] (a : A)
			(
				implicit
				active : Case.Aux[
					PolyT,
					$aggregate$Status.Active.type :: A :: HNil,
					R
					],

				@unused inactive : Case.Aux[
					PolyT,
					$aggregate$Status.Inactive.type :: A :: HNil,
					R
					]
			)
			: R =
			active (this, a)


		override protected def allow (candidate : $aggregate$Status) : Boolean =
			candidate === Inactive
	}


	case object Inactive
		extends $aggregate$Status
	{
		override def pfmap[PolyT <: Poly2, A, R] (a : A)
			(
				implicit
				@unused active : Case.Aux[
					PolyT,
					$aggregate$Status.Active.type :: A :: HNil,
					R
					],

				inactive : Case.Aux[
					PolyT,
					$aggregate$Status.Inactive.type :: A :: HNil,
					R
					]
			)
			: R =
			inactive (this, a)


		override def allow (candidate : $aggregate$Status) : Boolean =
			candidate === Active
	}


	/// Instance Properties
	val values = findValues


	/// Implicit Conversions
	implicit val $name;format="camel"$StatusDiff : Diff[$aggregate$Status] =
		Diff.derived[$aggregate$Status]
}

