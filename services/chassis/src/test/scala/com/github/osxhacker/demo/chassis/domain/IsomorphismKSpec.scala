package com.github.osxhacker.demo.chassis.domain

import cats.data.{
	NonEmptyChain,
	ValidatedNec
	}

import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec

import com.github.osxhacker.demo.chassis.ProjectSpec


/**
 * The '''IsomorphismKSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.domain.IsomorphismK]] for fitness of
 * purpose and serves as an exemplar of its use.
 */
final class IsomorphismKSpec ()
	extends AnyWordSpec
		with Diagrams
		with ProjectSpec
		with NaturalTransformations
{
	/// Class Imports
	import cats.syntax.either._
	import cats.syntax.validated._


	"The IsomorphismK functional type" must {
		"support a valid F[_] to G[_] transformation" in {
			val valid = "foo".validNec[Throwable]
			val iso = IsomorphismK[ValidatedNec[Throwable, *], ErrorOr] ()
			val result = iso.to (valid)

			assert (result.isRight)
			}

		"support a valid G[_] to F[_] transformation" in {
			val valid = "foo".asRight[Throwable]
			val iso = IsomorphismK[ValidatedNec[Throwable, *], ErrorOr] ()
			val result = iso.reverse.to (valid)

			assert (result.isValid)
			}

		"support an invalid F[_] to G[_] transformation" in {
			val errors = NonEmptyChain (
				new RuntimeException ("first problem"),
				new RuntimeException ("second problem")
				)

			val iso = IsomorphismK[ValidatedNec[Throwable, *], ErrorOr] ()
			val result = iso.to (errors.invalid[String])

			assert (result.isLeft)
			assert (result.swap.exists (_.getMessage contains "first problem"))
			assert (result.swap.exists (_.getMessage contains "second problem"))
			}

		"support an invalid G[_] to F[_] transformation" in {
			val invalid = new RuntimeException ().asLeft[String]
			val iso = IsomorphismK[ValidatedNec[Throwable, *], ErrorOr] ()
			val result = iso.reverse.to (invalid)

			assert (result.isInvalid)
			}
		}
}

