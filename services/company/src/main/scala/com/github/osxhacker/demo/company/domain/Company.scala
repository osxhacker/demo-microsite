package com.github.osxhacker.demo.company.domain

import java.util.Objects

import scala.annotation.unused

import cats.{
	ApplicativeThrow,
	Eq
	}

import shapeless.{
	syntax => _,
	_
	}

import shapeless.ops.coproduct
import com.softwaremill.diffx
import com.softwaremill.diffx.Diff
import eu.timepit.refined
import eu.timepit.refined.api.RefinedTypeOps
import monocle.macros.Lenses

import com.github.osxhacker.demo.chassis.domain.{
	Slug,
	Specification
	}

import com.github.osxhacker.demo.chassis.domain.algorithm.InferDomainEvents
import com.github.osxhacker.demo.chassis.domain.entity._
import com.github.osxhacker.demo.chassis.domain.error.DomainValueError
import com.github.osxhacker.demo.company.domain.ScopedEnvironment
import com.github.osxhacker.demo.company.domain.event.{
	CompanyCreated => _,
	CompanyDeleted => _,
	_
	}


/**
 * The '''Company''' type defines the Domain Object Model representation of
 * Company.  It is a model of an aggregate root and, as such, equality is
 * determined strictly by its `id` and `version`.
 */
@Lenses ()
final case class Company (
	val id : Identifier[Company],
	val version : Version,
	val slug : Slug,
	val name : Company.Name,
	val status : CompanyStatus,
	val description : Company.Description,
	val timestamps : ModificationTimes
	)
{
	/// Class Imports
	import Company.SupportedEvents
	import cats.syntax.either._
	import cats.syntax.eq._
	import cats.syntax.functor._
	import diffx.compare
	import mouse.boolean._


	override def equals (that : Any) : Boolean =
		canEqual (that) && this === that.asInstanceOf[Company]


	override def hashCode () : Int = Objects.hash (id, version)


	/**
	 * The changeStatusTo method attempts to update '''this''' `status` to be
	 * the '''desired''' given, if the current
	 * [[com.github.osxhacker.demo.company.domain.CompanyStatus]]
	 * allows it.
	 */
	def changeStatusTo[F[_]] (desired : CompanyStatus)
		(implicit applicativeThrow : ApplicativeThrow[F])
		: F[Company] =
		when (_.status.canBecome (desired)) (_.copy (status = desired))
			.toRight (
				DomainValueError (
					s"cannot change status from '$status' to '$desired'"
					)
				)
			.liftTo[F]


	/**
	 * The differsFrom method determines whether or not '''this''' instance is
	 * different than the '''other''' one given.  It takes into account
	 * properties to ignore and custom comparisons (as needed) as determined by
	 * the `implicit` '''algorithm'''.
	 */
	def differsFrom (other : Company)
		(implicit algorithm : Diff[Company])
		: Boolean =
		compare (this, other).isIdentical === false


	/**
	 * The infer method instantiates an
	 * [[com.github.osxhacker.demo.chassis.domain.algorithm.InferDomainEvents]]
	 * with '''this''' and the '''other''' '''Company''', requiring the
	 * invocation to provide what ''AllowableEventsT'' are allowed to be
	 * emitted.
	 */
	def infer[AllowableEventsT <: Coproduct] (other : Company)
		(
			implicit
			@unused
			isSubset : coproduct.Basis[CompanyChangeEvents, AllowableEventsT],
		)
		: InferDomainEvents[Company, SupportedEvents.type, AllowableEventsT] =
		new InferDomainEvents[
			Company,
			SupportedEvents.type,
			AllowableEventsT
			] (this, other)


	/**
	 * The touch method attempts to increment the `version` and ensure that the
	 * `timestamps` are `touch`ed as well.
	 */
	def touch[F[_]] ()
		(implicit applicativeThrow : ApplicativeThrow[F])
		: F[Company] =
		version.next[F] ()
			.map {
				Company.version
					.replace (_)
					.andThen (
						Company.timestamps
							.modify (ModificationTimes.touch)
						)
					.apply (this)
				}


	/**
	 * The unless method is a higher-kinded functor which conditionally invokes
	 * the given functor '''f''' with '''this''' iff `specification` evaluates
	 * '''this''' to be `false`.
	 */
	def unless[A] (specification : Specification[Company])
		(f : Company => A)
		: Option[A] =
		when (!specification) (f)


	/**
	 * The when method is a higher-kinded functor which conditionally invokes
	 * the given functor '''f''' with '''this''' iff the '''specification'''
	 * evaluates '''this''' to be `true`.
	 */
	def when[A] (specification : Specification[Company])
		(f : Company => A)
		: Option[A] =
		specification (this).option (f (this))
}


object Company
{
	/// Class Imports
	import diffx.generic.auto._
	import diffx.refined._
	import refined.api.Refined
	import refined.auto._
	import refined.boolean.And
	import refined.collection.{
		MaxSize,
		Size
		}

	import refined.numeric.Interval
	import refined.string.Trimmed


	/// Class Types
	type Description = Refined[
		String,
		Trimmed And
			MaxSize[2048]
		]


	type Name = Refined[
		String,
		Trimmed And
			Size[Interval.Closed[2, 64]]
		]


	object Description
		extends RefinedTypeOps[Description, String]


	object Name
		extends RefinedTypeOps[Name, String]


	/**
	 * The '''SupportedEvents''' [[shapeless.Poly2]] type provides the ability
	 * to produce
	 * [[com.github.osxhacker.demo.company.domain.event.CompanyChangeEvents]]
	 * based on a `from` / `to` pair, which represents what a
	 * [[com.github.osxhacker.demo.company.domain.Company]] was ("from") and
	 * what it has become ("to").
	 */
	object SupportedEvents
		extends Poly2
	{
		/// Class Imports
		import Option.unless
		import cats.syntax.eq._
		import refined.cats._


		/// Class Types
		type ResultType[A] = Case.Aux[Company, Company, Option[A]]


		/// Implicit Conversions
		implicit def caseProfileChanged[F[_]] (
			implicit env : ScopedEnvironment[F]
			)
			: ResultType[CompanyProfileChanged] =
			at {
				(from, to) =>
					from.when (_.differsFrom (to) (profileProperties)) {
						_ =>
							CompanyProfileChanged (to)
						}
				}

		implicit def caseSlugChanged[F[_]] (implicit env : ScopedEnvironment[F])
			: ResultType[CompanySlugChanged] =
			at {
				(from, to) =>
					unless (from.slug === to.slug) {
						CompanySlugChanged (to, from.slug)
						}
				}

		implicit def caseStatusChanged[F[_]] (
			implicit env : ScopedEnvironment[F]
			)
			: ResultType[CompanyStatusChanged] =
			at {
				(from, to) =>
					unless (from.status === to.status) {
						CompanyStatusChanged (to)
						}
				}
	}


	/// Instance Properties
	private lazy val profileProperties : Diff[Company] =
		companyDiff.ignore (_.slug)
			.ignore (_.status)


	/// Implicit Conversions
	implicit val companyDiff : Diff[Company] =
		Diff.derived[Company]
			.ignore (_.timestamps)

	implicit val companyEq : Eq[Company] =
		Eq.and[Company] (Eq.by (_.id), Eq.by (_.version))

	implicit val companyNamespace : Identifier.EntityNamespace[Company] =
		Identifier.namespaceFor[Company] ("company")
}

