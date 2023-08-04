package com.github.osxhacker.demo.chassis.domain.entity

import cats.{
	ApplicativeThrow,
	Eq,
	Show
	}

import com.softwaremill.diffx
import com.softwaremill.diffx.Diff
import eu.timepit.refined
import eu.timepit.refined.types.numeric.PosInt
import monocle.Getter


/**
 * The '''Version''' type defines the Domain Object Model concept of a positive
 * integer representing a specific version of an entity or aggregate root.
 */
final case class Version (private val value : PosInt)
	extends Ordered[Version]
{
	override def compare (that : Version) = value.value - that.value.value


	/**
	 * The next method attempts to increment the current `value` by `1` with
	 * ''Int'' wrapping detection.
	 */
	def next[F[_]] ()
		(implicit applicativeThrow : ApplicativeThrow[F])
		: F[Version] =
		Version[F] (value.value + 1)
}


object Version
{
	/// Class Imports
	import cats.syntax.either._
	import diffx.refined._
	import refined.auto._


	/// Instance Properties
	val initial : Version = Version (1)
	val value = Getter[Version, PosInt] (_.value)


	/**
	 * The apply method is provided to support functional-style creation if and
	 * only if the '''candidate''' ''Int'' is a positive value
	 * (`1 .. Int.MaxValue`).
	 */
	def apply[F[_]] (candidate : Int)
		(implicit applicativeThrow : ApplicativeThrow[F])
		: F[Version] =
		PosInt.from (candidate)
			.bimap (
				new IllegalArgumentException (_),
				new Version (_)
				)
			.liftTo[F]


	/// Implicit Conversions
	implicit val versionDiff : Diff[Version] = Diff.derived
	implicit val versionEq : Eq[Version] = Eq.fromUniversalEquals
	implicit val versionShow : Show[Version] = Show.show (_.value.toString ())
}

