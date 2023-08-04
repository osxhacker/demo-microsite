package com.github.osxhacker.demo.chassis.domain.entity

import cats.Eq

import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec

import com.github.osxhacker.demo.chassis.ProjectSpec


/**
 * The '''ModificationTimesSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.domain.entity.ModificationTimes]] for
 * fitness of purpose and serves as an exemplar of its use.
 */
final class ModificationTimesSpec ()
	extends AnyWordSpec
		with Diagrams
		with ProjectSpec
{
	/// Class Imports
	import scala.math.Ordered._


	"The ModificationTimes value type" must {
		"support cats.Eq" in {
			val first = ModificationTimes.now ()

			Thread.sleep (50L)

			val second = ModificationTimes.now ()

			assert (Eq[ModificationTimes].eqv (first, first))
			assert (Eq[ModificationTimes].neqv (first, second))
			}

		"support cats.Show" in {
			assertCompiles (
				"""
					import cats.syntax.show._

					ModificationTimes.now ().show
				"""
				)
			}

		"ensure 'now' produces equivalent 'createdOn' and 'lastChanged'" in {
			val times = ModificationTimes.now ()

			assert (times.createdOn === times.lastChanged)
			assert (
				ModificationTimes.modificationTimesDiff (times, times).isIdentical
				)
			}

		"ensure 'touch' produces a new 'lastChanged' value" in {
			val original = ModificationTimes.now ()
			val first = original.touch ()

			Thread.sleep (50L)

			val second = original.touch ()

			assert (first.createdOn === second.createdOn)
			assert (first.lastChanged < second.lastChanged)
			assert (
				!ModificationTimes.modificationTimesDiff (first, second).isIdentical
				)
			}
		}
}

