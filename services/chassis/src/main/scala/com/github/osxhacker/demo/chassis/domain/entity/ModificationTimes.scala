package com.github.osxhacker.demo.chassis.domain.entity

import java.time.Instant

import cats.{
	Eq,
	Endo,
	Show
	}

import com.softwaremill.diffx.Diff
import monocle.macros.Lenses


/**
 * The '''ModificationTimes''' type defines the Domain Object Model concept of
 * when an entity or aggregate root was created and the last time it was
 * modified.
 */
@Lenses ()
final case class ModificationTimes (
	val createdOn : Instant,
	val lastChanged : Instant
	)
{
	/**
	 * The touch method behaves similar to the Unix `touch` command in that the
	 * `lastChanged` property is made to be `now`.
	 */
	def touch () : ModificationTimes = ModificationTimes.touch (this)
}


object ModificationTimes
{
	/// Instance Properties
	/**
	 * The touch property defines an [[cats.Endo]] which ensures that a
	 * '''ModificationTimes''' instance properly reflects being altered at the
	 * moment the [[cats.Endo]] is evaluated.  It is defined in terms of
	 * `modify`, which ignores the original value, so that `Instant.now` is
	 * evaluated __each time__ touch is used.
	 */
	val touch : Endo[ModificationTimes] =
		lastChanged.modify (_ => Instant.now ())


	/**
	 * The now method is provided to support functional-style creation of a
	 * '''ModificationTimes''' instance having both `createdOn` and
	 * `lastChanged` being set to the __same__ `Instant.now` value.
	 */
	def now () : ModificationTimes =
	{
		val thisInstant = Instant.now ()

		ModificationTimes (thisInstant, thisInstant)
	}


	/// Implicit Conversions
	implicit val modificationTimesDiff : Diff[ModificationTimes] =
		Diff.derived[ModificationTimes]

	implicit val modificationTimesEq : Eq[ModificationTimes] =
		Eq.fromUniversalEquals

	implicit val modificationTimesShow : Show[ModificationTimes] = Show.show {
		times =>
			s"(${times.createdOn},${times.lastChanged})"
		}
}

