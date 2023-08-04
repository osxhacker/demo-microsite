package com.github.osxhacker.demo.chassis.domain.algorithm

import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec

import com.github.osxhacker.demo.chassis.domain.ErrorOr


/**
 * The '''GenerateServiceFingerprintSpec''' type defines the unit-tests which
 * certify [[com.github.osxhacker.demo.chassis.domain.entity.Identifier]] for
 * fitness of purpose and serves as an exemplar of its use.
 */
final class GenerateServiceFingerprintSpec ()
	extends AnyWordSpec
		with Diagrams
{
	/// Class Imports
	import cats.syntax.show._


	"The GenerateServiceFingerprint algorithm" must {
		"be able to generate a SHA-256 fingerprint" in {
			val result = GenerateServiceFingerprint[ErrorOr] ()

			assert (result.isRight)
			assert (result.forall (_.show.matches ("""^[a-f0-9]{64}$""")))
			}
		}
}

