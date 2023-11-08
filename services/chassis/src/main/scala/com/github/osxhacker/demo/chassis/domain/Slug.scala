package com.github.osxhacker.demo.chassis.domain

import cats.{
	ApplicativeThrow,
	Eq,
	Order,
	Show
	}

import com.softwaremill.diffx
import com.softwaremill.diffx.Diff
import eu.timepit.refined
import eu.timepit.refined.api.RefinedTypeOps
import monocle.Getter


/**
 * The '''Slug''' type defines the Domain Object Model concept of a
 * [[https://en.wikipedia.org/wiki/Clean_URL#Slug slug]].  Here, a '''Slug'''
 * has the format of "word-another-...".  For example, a company named "Acme
 * Shoes International" may have a '''Slug''' of `acme-shoes` or
 * `acme-shoes-international`.
 */
final case class Slug (private val value : Slug.Value)
	extends Ordered[Slug]
{
	override def compare (that : Slug) : Int =
		value.value.compareTo (that.value.value)
}


object Slug
{
	/// Class Imports
	import cats.syntax.either._
	import diffx.refined._
	import refined.api.Refined
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
		Size[Interval.Closed[2, 64]] And
			Trimmed And
			MatchesRegex[
				"^[a-z](?:[a-z0-9]+|(?:[a-z0-9]*(?:-[a-z0-9]+)+))$"
				]
		]


	object Value
		extends RefinedTypeOps[Value, String]


	/// Instance Properties
	val value = Getter[Slug, Value] (_.value)


	/**
	 * The apply method is provided to support functional-style creation within
	 * the context ''F''.
	 */
	def apply[F[_]] (candidate : String)
		(implicit applicativeThrow : ApplicativeThrow[F])
		: F[Slug] =
		Value.from (candidate)
			.bimap (
				new IllegalArgumentException (_),
				new Slug (_)
				)
			.liftTo[F]


	/// Implicit Conversions
	implicit val slugDiff : Diff[Slug] = Diff.derived
	implicit val slugEq : Eq[Slug] = Eq.fromUniversalEquals
	implicit val slugOrder : Order[Slug] = Order.fromOrdering
	implicit val slugShow : Show[Slug] = Show.show (_.value.toString ())
}

