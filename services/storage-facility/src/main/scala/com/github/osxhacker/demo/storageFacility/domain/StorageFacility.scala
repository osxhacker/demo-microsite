package com.github.osxhacker.demo.storageFacility.domain

import scala.annotation.unused
import scala.language.postfixOps

import java.util.Objects

import cats.{
	ApplicativeThrow,
	Eq
	}

import com.softwaremill.diffx
import com.softwaremill.diffx.Diff
import eu.timepit.refined
import eu.timepit.refined.api.RefinedTypeOps
import monocle.macros.Lenses
import shapeless.{
	syntax => _,
	_
	}

import shapeless.ops.coproduct
import squants.space.Volume

import com.github.osxhacker.demo.chassis.domain.Specification
import com.github.osxhacker.demo.chassis.domain.algorithm.InferDomainEvents
import com.github.osxhacker.demo.chassis.domain.entity._
import com.github.osxhacker.demo.chassis.domain.error.DomainValueError
import com.github.osxhacker.demo.chassis.domain.event.Region
import com.github.osxhacker.demo.storageFacility.domain.event._


/**
 * The '''StorageFacility''' type defines the Domain Object Model representation
 * of a single storage facility.  It is a model of an aggregate root and, as
 * such, equality is determined strictly by its `id` and `version`.
 */
@Lenses ()
final case class StorageFacility (
	val id : Identifier[StorageFacility],
	val version : Version,
	val owner : Company,
	val status : StorageFacilityStatus,
	val name : StorageFacility.Name,
	val city : StorageFacility.City,
	val state : StorageFacility.State,
	val zip : StorageFacility.Zip,
	val capacity : Volume,
	val available : Volume,
	val timestamps : ModificationTimes,

	/**
	 * The primary property serves as an exemplar of how an entity can be
	 * enhanced after the original model has been put into production.  In this
	 * case, primary reifies which
	 * [[com.github.osxhacker.demo.chassis.domain.event.Region]] is responsible
	 * for the '''StorageFacility''' lifecycle.
	 */
	val primary : Option[Region] = None
	)
{
	/// Class Imports
	import StorageFacility.SupportedEvents
	import cats.syntax.either._
	import cats.syntax.eq._
	import cats.syntax.functor._
	import diffx.compare
	import mouse.boolean._


	override def equals (that : Any) : Boolean =
		canEqual (that) && this === that.asInstanceOf[StorageFacility]


	override def hashCode () : Int = Objects.hash (id, version)


	/**
	 * This version of the belongsTo method determines whether or not '''this'''
	 * `owner` corresponds to the given '''candidate'''
	 * [[com.github.osxhacker.demo.storageFacility.domain.Company]].
	 */
	def belongsTo (candidate : Company) : Boolean =
		belongsTo (candidate.toRef ())


	/**
	 * This version of the belongsTo method determines whether or not '''this'''
	 * `owner` is identified by the given '''candidate'''
	 * [[com.github.osxhacker.demo.storageFacility.domain.CompanyReference]].
	 */
	def belongsTo (candidate : CompanyReference) : Boolean =
		owner.toRef () === candidate


	/**
	 * The changeStatusTo method attempts to update '''this''' `status` to be
	 * the '''candidate''' given, if the current
	 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacilityStatus]]
	 * allows it.
	 */
	def changeStatusTo[F[_]] (candidate : StorageFacilityStatus)
		(implicit applicativeThrow : ApplicativeThrow[F])
		: F[StorageFacility] =
		Either.cond (
			status.canBecome (candidate),
			copy (status = candidate),
			DomainValueError (
				s"cannot change status from '$status' to '$candidate'"
				)
			)
			.liftTo[F]


	/**
	 * The definedIn method determines whether or not '''this''' instance has as
	 * its `primary` [[com.github.osxhacker.demo.chassis.domain.event.Region]]
	 * the given '''region''' or not, if the `primary`
	 * [[com.github.osxhacker.demo.chassis.domain.event.Region]] is known.  If
	 * the `primary` [[com.github.osxhacker.demo.chassis.domain.event.Region]]
	 * is __not__ known, the '''default''' value is reported.
	 */
	def definedIn (region : Region, default : Boolean = true) : Boolean =
		primary.fold (default) (_ === region)


	/**
	 * The differsFrom method determines whether or not '''this''' instance is
	 * different than the '''other''' one given.  It takes into account
	 * properties to ignore and custom comparisons (as needed) as determined by
	 * the `implicit` '''algorithm'''.
	 */
	def differsFrom (other : StorageFacility)
		(implicit algorithm : Diff[StorageFacility])
		: Boolean =
		compare (this, other).isIdentical === false


	/**
	 * The infer method instantiates an
	 * [[com.github.osxhacker.demo.chassis.domain.algorithm.InferDomainEvents]]
	 * with '''this''' and the '''other''' '''StorageFacility''', requiring the
	 * invocation to provide what ''AllowableEventsT'' are allowed to be
	 * emitted.
	 */
	def infer[AllowableEventsT <: Coproduct] (other : StorageFacility)
		(
			implicit
			@unused
			isSubset : coproduct.Basis[
				StorageFacilityChangeEvents,
				AllowableEventsT
				]
		)
		: InferDomainEvents[
			StorageFacility,
			SupportedEvents.type,
			AllowableEventsT
			] =
		new InferDomainEvents[
			StorageFacility,
			SupportedEvents.type,
			AllowableEventsT
			] (this, other)


	/**
	 * The touch method attempts to increment the `version` and ensure that the
	 * `timestamps` are `touch`ed as well.
	 */
	def touch[F[_]] ()
		(implicit applicativeThrow : ApplicativeThrow[F])
		: F[StorageFacility] =
		version.next[F] ()
			.map {
				StorageFacility.version
					.replace (_)
					.andThen (
						StorageFacility.timestamps
							.modify (ModificationTimes.touch)
						)
					.apply (this)
				}


	/**
	 * The unless method is a higher-kinded functor which conditionally invokes
	 * the given functor '''f''' with '''this''' iff `specification` evaluates
	 * '''this''' to be `false`.
	 */
	def unless[A] (specification : Specification[StorageFacility])
		(f : StorageFacility => A)
		: Option[A] =
		when (!specification) (f)


	/**
	 * The when method is a higher-kinded functor which conditionally invokes
	 * the given functor '''f''' with '''this''' iff the '''specification'''
	 * evaluates '''this''' to be `true`.
	 */
	def when[A] (specification : Specification[StorageFacility])
		(f : StorageFacility => A)
		: Option[A] =
		specification (this).option (f (this))
}


object StorageFacility
{
	/// Class Imports
	import cats.syntax.eq._
	import diffx.generic.auto._
	import diffx.refined._
	import refined.api.Refined
	import refined.auto._
	import refined.boolean.{
		And,
		Or
		}

	import refined.char.{
		Digit,
		UpperCase
		}

	import refined.collection.{
		Forall,
		NonEmpty,
		Size
		}

	import refined.generic.Equal
	import refined.numeric.Interval
	import refined.string.{
		MatchesRegex,
		Trimmed
		}


	/// Class Types
	type City = Refined[
		String,
		Size[Interval.Closed[2, 32]] And
			MatchesRegex["^[A-Za-z'][A-Za-z'. -]*[a-z]$"]
		]


	type Name = Refined[
		String,
		Trimmed And NonEmpty
		]


	type State = Refined[
		String,
		Size[Interval.Closed[2, 3]] And Forall[UpperCase]
		]


	type Zip = Refined[
		String,
		Or[
			Size[Equal[5]] And Forall[Digit],
			Size[Equal[10]] And MatchesRegex["^[0-9]{5}-[0-9]{4}$"]
			]
		]


	object City
		extends RefinedTypeOps[City, String]


	object Name
		extends RefinedTypeOps[Name, String]


	object State
		extends RefinedTypeOps[State, String]


	/**
	 * The '''SupportedEvents''' [[shapeless.Poly2]] type provides the ability
	 * to produce __all__
	 * [[com.github.osxhacker.demo.storageFacility.domain.event.StorageFacilityChangeEvents]]
	 * based on a `from` / `to` pair, which represents what a
	 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]] was
	 * ("from") and what it has become ("to").
	 */
	object SupportedEvents
		extends Poly2
	{
		/// Class Types
		type ResultType[A] = Case.Aux[
			StorageFacility,
			StorageFacility,
			Option[A]
			]


		/// Implicit Conversions
		implicit def caseProfileChanged[F[_]] (
			implicit env : ScopedEnvironment[F]
			)
			: ResultType[StorageFacilityProfileChanged] =
			at {
				(from, to) =>
					from.when (_.differsFrom (to) (profileProperties)) {
						_ =>
							StorageFacilityProfileChanged (to)
					}
				}


		implicit def caseStatusChanged[F[_]] (
			implicit env : ScopedEnvironment[F]
			)
			: ResultType[StorageFacilityStatusChanged] =
			at {
				(from, to) =>
					to.unless (_.status === from.status) {
						StorageFacilityStatusChanged (_)
						}
				}
	}


	object Zip
		extends RefinedTypeOps[Zip, String]


	/// Instance Properties
	private lazy val profileProperties : Diff[StorageFacility] =
		storageFacilityDiff.ignore (_.owner)
			.ignore (_.status)
			.ignore (_.capacity)
			.ignore (_.available)
			.ignore (_.primary)


	/// Implicit Conversions
	implicit val storageFacilityDiff : Diff[StorageFacility] = {
		implicit val volumeDiff : Diff[Volume] = Diff.useEquals

		Diff.derived[StorageFacility]
			.ignore (_.timestamps)
		}

	implicit val storageFacilityEq : Eq[StorageFacility] =
		Eq.and[StorageFacility] (Eq.by (_.id), Eq.by (_.version))

	implicit val storageFacilityNamespace
		: Identifier.EntityNamespace[StorageFacility] =
		Identifier.namespaceFor[StorageFacility] ("storage-facility")
}

