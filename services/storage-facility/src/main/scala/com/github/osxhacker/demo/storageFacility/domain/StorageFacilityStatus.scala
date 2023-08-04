package com.github.osxhacker.demo.storageFacility.domain

import scala.annotation.unused

import com.softwaremill.diffx.Diff
import enumeratum._
import shapeless.{
	syntax => _,
	_
	}

import shapeless.PolyDefns.Case


/**
 * The '''StorageFacilityStatus''' type defines the Domain Object Model
 * representation of __all__ discrete
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]] status
 * indicators.  Note that some transitions may be disallowed due to domain
 * rules.
 */
sealed trait StorageFacilityStatus
	extends EnumEntry
{
	/// Class Imports
	import cats.syntax.eq._


	/**
	 * The canBecome method determines if '''this''' [[enumeratum.EnumEntry]]
	 * allows a transition to the '''desired''' '''StorageFacilityStatus'''.
	 * Transitioning to the same '''StorageFacilityStatus''' is always allowed.
	 */
	final def canBecome (desired : StorageFacilityStatus) : Boolean =
		this === desired || allow (desired)


	/**
	 * The pfmap method defines a polymorphic functor `map`ping of all
	 * '''StorageFacilityStatus'''es to a common result type ''R''.  In essence, it is a
	 * functional programming form of the
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
	 *         implicit val caseSuspended : Case.Aux[Suspended.type, A, R] = ???
	 *     }
	 *
	 *     ...
	 *
	 *     val context : A = ???
	 *     val result : R = aFacility.status
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
				StorageFacilityStatus.Active.type :: A :: HNil,
				R
				],

			closed : Case.Aux[
				PolyT,
				StorageFacilityStatus.Closed.type :: A :: HNil,
				R
				],

			underConstruction : Case.Aux[
				PolyT,
				StorageFacilityStatus.UnderConstruction.type :: A :: HNil,
				R
				]
		)
		: R


	/**
	 * The allow abstract method is a model of the TEMPLATE pattern and is only
	 * invoked when '''this''' instance is __not__ the '''desired''' one given
	 * to `canBecome` in order to complete the decision process.
	 */
	protected def allow (desired : StorageFacilityStatus) : Boolean
}


object StorageFacilityStatus
	extends Enum[StorageFacilityStatus]
		with CatsEnum[StorageFacilityStatus]
{
	/// Class Imports
	import cats.syntax.eq._


	/// Class Types
	case object Active
		extends StorageFacilityStatus
	{
		override def pfmap[PolyT <: Poly2, A, R] (a : A)
			(
				implicit
				active : Case.Aux[
					PolyT,
					StorageFacilityStatus.Active.type :: A :: HNil,
					R
					],

				@unused closed : Case.Aux[
					PolyT,
					StorageFacilityStatus.Closed.type :: A :: HNil,
					R
					],

				@unused underConstruction : Case.Aux[
					PolyT,
					StorageFacilityStatus.UnderConstruction.type :: A :: HNil,
					R
					]
			)
			: R =
			active (this, a)


		override protected def allow (candidate : StorageFacilityStatus)
			: Boolean =
			candidate === Closed
	}


	case object Closed
		extends StorageFacilityStatus
	{
		override def pfmap[PolyT <: Poly2, A, R] (a : A)
			(
				implicit
				@unused active : Case.Aux[
					PolyT,
					StorageFacilityStatus.Active.type :: A :: HNil,
					R
					],

				closed : Case.Aux[
					PolyT,
					StorageFacilityStatus.Closed.type :: A :: HNil,
					R
					],

				@unused underConstruction : Case.Aux[
					PolyT,
					StorageFacilityStatus.UnderConstruction.type :: A :: HNil,
					R
					]
			)
			: R =
			closed (this, a)


		override protected def allow (candidate : StorageFacilityStatus)
			: Boolean =
			candidate === Active
	}


	case object UnderConstruction
		extends StorageFacilityStatus
	{
		override def pfmap[PolyT <: Poly2, A, R] (a : A)
			(
				implicit
				@unused active : Case.Aux[
					PolyT,
					StorageFacilityStatus.Active.type :: A :: HNil,
					R
					],

				@unused closed : Case.Aux[
					PolyT,
					StorageFacilityStatus.Closed.type :: A :: HNil,
					R
					],

				underConstruction : Case.Aux[
					PolyT,
					StorageFacilityStatus.UnderConstruction.type :: A :: HNil,
					R
					]
			)
			: R =
			underConstruction (this, a)


		override protected def allow (candidate : StorageFacilityStatus)
			: Boolean =
			candidate === Active
	}


	/// Instance Properties
	val values = findValues


	/// Implicit Conversions
	implicit val storageFacilityStatusDiff : Diff[StorageFacilityStatus] =
		Diff.derived[StorageFacilityStatus]
}

