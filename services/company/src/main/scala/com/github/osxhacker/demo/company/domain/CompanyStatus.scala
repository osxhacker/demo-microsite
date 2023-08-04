package com.github.osxhacker.demo.company.domain

import scala.annotation.unused

import com.softwaremill.diffx.Diff
import enumeratum._
import shapeless.{
	syntax => _,
	_
	}

import shapeless.PolyDefns.Case


/**
 * The '''CompanyStatus''' type defines the Domain Object Model
 * representation of __all__ discrete
 * [[com.github.osxhacker.demo.company.domain.Company]] status
 * indicators.  Note that some transitions may be disallowed due to domain
 * rules.
 */
sealed trait CompanyStatus
	extends EnumEntry
{
	/// Class Imports
	import cats.syntax.eq._


	/**
	 * The canBecome method determines if '''this''' [[enumeratum.EnumEntry]]
	 * allows a transition to the '''desired''' '''CompanyStatus'''.
	 * Transitioning to the same '''CompanyStatus''' is always allowed.
	 */
	final def canBecome (desired : CompanyStatus) : Boolean =
		this === desired || allow (desired)


	/**
	 * The pfmap method defines a polymorphic functor `map`ping of all
	 * '''CompanyStatus'''es to a common result type ''R''.  In essence, it is a
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
	 *     val result : R = aCompany.status
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
			active : Case.Aux[PolyT, CompanyStatus.Active.type :: A :: HNil, R],
			inactive : Case.Aux[
				PolyT,
				CompanyStatus.Inactive.type :: A :: HNil,
				R
				],

			suspended : Case.Aux[
				PolyT,
				CompanyStatus.Suspended.type :: A :: HNil,
				R
				]
		)
		: R


	/**
	 * The allow abstract method is a model of the TEMPLATE pattern and is only
	 * invoked when '''this''' instance is __not__ the '''desired''' one given
	 * to `canBecome` in order to complete the decision process.
	 */
	protected def allow (desired : CompanyStatus) : Boolean
}


object CompanyStatus
	extends Enum[CompanyStatus]
		with CatsEnum[CompanyStatus]
{
	/// Class Imports
	import cats.syntax.eq._


	/// Class Types
	case object Active
		extends CompanyStatus
	{
		override def pfmap[PolyT <: Poly2, A, R] (a : A)
			(
				implicit
				active : Case.Aux[
					PolyT,
					CompanyStatus.Active.type :: A :: HNil,
					R
					],

				@unused inactive : Case.Aux[
					PolyT,
					CompanyStatus.Inactive.type :: A :: HNil,
					R
					],

				@unused suspended : Case.Aux[
					PolyT,
					CompanyStatus.Suspended.type :: A :: HNil,
					R
					]
			)
			: R =
			active (this, a)


		override protected def allow (candidate : CompanyStatus) : Boolean =
			true
	}


	case object Inactive
		extends CompanyStatus
	{
		override def pfmap[PolyT <: Poly2, A, R] (a : A)
			(
				implicit
				@unused active : Case.Aux[
					PolyT,
					CompanyStatus.Active.type :: A :: HNil,
					R
					],

				inactive : Case.Aux[
					PolyT,
					CompanyStatus.Inactive.type :: A :: HNil,
					R
					],

				@unused suspended : Case.Aux[
					PolyT,
					CompanyStatus.Suspended.type :: A :: HNil,
					R
					]
			)
			: R =
			inactive (this, a)


		override protected def allow (candidate : CompanyStatus) : Boolean =
			candidate === Active
	}


	case object Suspended
		extends CompanyStatus
	{
		override def pfmap[PolyT <: Poly2, A, R] (a : A)
			(
				implicit
				@unused active : Case.Aux[
					PolyT,
					CompanyStatus.Active.type :: A :: HNil,
					R
					],

				@unused inactive : Case.Aux[
					PolyT,
					CompanyStatus.Inactive.type :: A :: HNil,
					R
					],

				suspended : Case.Aux[
					PolyT,
					CompanyStatus.Suspended.type :: A :: HNil,
					R
					]
			)
			: R =
			suspended (this, a)


		override protected def allow (candidate : CompanyStatus) : Boolean =
			candidate === Active
	}


	/// Instance Properties
	val values : IndexedSeq[CompanyStatus] = findValues


	/// Implicit Conversions
	implicit val companyStatusDiff : Diff[CompanyStatus] =
		Diff.derived[CompanyStatus]
}

