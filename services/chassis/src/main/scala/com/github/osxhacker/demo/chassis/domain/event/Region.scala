package com.github.osxhacker.demo.chassis.domain.event

import cats.{
	ApplicativeThrow,
	Eq,
	Show
	}

import com.softwaremill.diffx
import com.softwaremill.diffx.Diff
import eu.timepit.refined
import eu.timepit.refined.api.{
	Refined,
	RefinedTypeOps
	}

import monocle.Getter


/**
 * The '''Region''' type reifies the concept of a grouping of microservices,
 * often by deployment geography, where distributed events having the same
 * '''Region''' __must__ originate from the same grouping.  The reason for
 * '''Region''' to exist is it is beneficial to be able to operate on a
 * heterogeneous set of events in a similar manner.  For example:
 *
 *   - Ignoring events which are from a different '''Region'''.
 *
 *   - Only processing events for a specific '''Region''' (such as logging).
 */
final case class Region (private val value : Region.Value)
	extends Ordered[Region]
{
	override def compare (that : Region) : Int =
		value.value.compareTo (that.value.value)
}


object Region
{
	/// Class Imports
	import cats.syntax.either._
	import diffx.refined._
	import refined.boolean.And
	import refined.collection.Size
	import refined.numeric.Interval
	import refined.string.{
		MatchesRegex,
		Trimmed
		}


	/// Class Types
	type Value = Refined[
		String,
		Trimmed And
			Size[Interval.Closed[2, 32]] And
			MatchesRegex[
				"^[a-z][a-z0-9]*(?:-[a-z0-9]+)*(?:-(?:dev|qa|prod|stage|[0-9]+))?$"
				]
		]


	object Value
		extends RefinedTypeOps[Value, String]


	/// Instance Properties
	val value = Getter[Region, Value] (_ .value)


	/**
	 * The apply method is provided to support functional-style creation from an
	 * arbitrary '''candidate'''.  If '''candidate''' does not conform to the
	 * supported
	 * [[com.github.osxhacker.demo.chassis.domain.event.Region.Value]], the
	 * problem is represented in ''F''.
	 */
	def apply[F[_]] (candidate : Refined[String, _])
		(implicit applicativeThrow : ApplicativeThrow[F])
		: F[Region] =
		Value.from (candidate.value)
			.bimap (
				new IllegalArgumentException (_),
				new Region (_)
				)
			.liftTo[F]


	/// Implicit Conversions
	implicit val regionDiff : Diff[Region] = Diff.derived
	implicit val regionEq : Eq[Region] = Eq.fromUniversalEquals
	implicit val regionShow : Show[Region] = Show.show (_.value.toString ())
}

