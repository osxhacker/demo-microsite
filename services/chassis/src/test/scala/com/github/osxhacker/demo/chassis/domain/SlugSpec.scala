package com.github.osxhacker.demo.chassis.domain

import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import com.github.osxhacker.demo.chassis.ProjectSpec


/**
 * The '''SlugSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.domain.Slug]] for fitness of
 * purpose and serves as an exemplar of its use.
 */
final class SlugSpec ()
	extends AnyWordSpec
		with Diagrams
		with ProjectSpec
		with ScalaCheckPropertyChecks
{
	"The Slug type" must {
		"accept well-formed content" in {
			assert (Slug[ErrorOr] ("yoda-panda").isRight)
			}

		"be able to detect and reject invalid content" in {
			/// Valid pattern: "^[a-z][a-z0-9]*(?:-[a-z0-9]+)*(?:-(?:dev|qa|prod|stage|[0-9]+))?$"
			val invalidSlugs = Table (
				/// Leading and trailing spaces are not allowed
				" abcde", "abcde ", "\tabcde", "abcde\t",

				/// Leading or only digits are not allowed
				"0-abcde", "123456789",

				/// Upper case characters are not allowed
				"ABCDE"
				)

			forAll (invalidSlugs) {
				candidate =>
					val result = Slug[ErrorOr] (candidate)

					assert (result.isLeft)
				}
			}

		"support total ordering" in {
			val first = Slug[ErrorOr] ("a-b-c")
			val second = Slug[ErrorOr] ("z-y")

			assert (first.isRight)
			assert (second.isRight)

			for {
				a <- first
				b <- second
				} yield a < b
			}
		}
}

