package com.github.osxhacker.demo.chassis.domain.entity

import cats.{
	Eq,
	Show
	}

import org.scalacheck.Gen
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.chassis.domain.ErrorOr


/**
 * The '''VersionSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.domain.entity.Version]] for fitness of
 * purpose and serves as an exemplar of its use.
 */
final class VersionSpec ()
	extends AnyWordSpec
		with Diagrams
		with ProjectSpec
		with ScalaCheckPropertyChecks
{
	/// Class Imports
	import cats.syntax.flatMap._


	"The Version type" must {
		"support values which are positive integers" in {
			val valid = Gen.chooseNum (1, Int.MaxValue)

			forAll (valid) {
				candidate =>
					val result = Version[ErrorOr] (candidate)

					assert (result.isRight)
				}
			}

		"disallow values which are not positive integers" in {
			val invalid = Gen.chooseNum (Int.MinValue, 0)

			forAll (invalid) {
				candidate =>
					val result = Version[ErrorOr] (candidate)

					assert (result.isLeft)
				}
			}

		"be able to produce the next Version when current < Int.MaxValue" in {
			val allowed = Gen.chooseNum (1, Int.MaxValue - 1)
				.map (Version[ErrorOr] (_))

			forAll (allowed) {
				current =>
					val incremented = current >>= (_.next[ErrorOr] ())

					assert (current.isRight)
					assert (incremented.isRight)
					assert (current.orFail () < incremented.orFail ())
				}
			}

		"be able to detect when the next version 'wraps around'" in {
			val max = Version[ErrorOr] (Int.MaxValue)
			val wrapped = max >>= (_.next[ErrorOr] ())

			assert (max.isRight)
			assert (wrapped.isLeft)
			}

		"support cats Eq" in {
			assertCompiles (
				"""
	   				implicitly[Eq[Version]]
				"""
				)
			}

		"support cats Show" in {
			assertCompiles (
				"""
	   				implicitly[Show[Version]]
				"""
				)
			}
		}
}

