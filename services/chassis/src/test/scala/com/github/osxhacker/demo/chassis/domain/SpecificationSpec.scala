package com.github.osxhacker.demo.chassis.domain

import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec


/**
 * The '''SpecificationSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.domain.Specification]] for fitness of
 * purpose and serves as an exemplar of its use.
 */
final class SpecificationSpec ()
	extends AnyWordSpec
		with Diagrams
{
	/// Class Types
	object EmptyString
		extends Specification[String]
	{
		override def apply (candidate : String) : Boolean = candidate.isEmpty
	}


	object IsLowerCase
		extends Specification[String]
	{
		override def apply (candidate : String) : Boolean =
			candidate.forall (_.isLower)
	}


	final case class NonEmptyString ()
		extends Specification[String]
	{
		override def apply (candidate : String) : Boolean = candidate.nonEmpty
	}


	"The Specification type" must {
		"support logical conjunction" in {
			val combined = NonEmptyString () && IsLowerCase

			assert (combined ("satisfies"))
			assert (!combined ("Will Not"))
			}

		"support logical disjunction" in {
			val combined = IsLowerCase || EmptyString

			assert (combined ("satisfies"))
			assert (combined (""))
			assert (!combined ("   "))
			}

		"support logical negation" in {
			assert (!EmptyString ("This has content"))
			assert (!(!EmptyString ("")))
			}
		}
}
