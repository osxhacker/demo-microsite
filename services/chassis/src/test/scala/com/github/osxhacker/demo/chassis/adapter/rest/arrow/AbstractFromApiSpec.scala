package com.github.osxhacker.demo.chassis.adapter.rest.arrow

import cats.data.{
	Kleisli,
	ValidatedNec
	}

import io.scalaland.chimney.TransformerF
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec
import com.github.osxhacker.demo.chassis.domain.{
	ErrorOr,
	NaturalTransformations
	}


/**
 * The '''AbstractFromApiSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.adapter.rest.arrow.AbstractFromApi]] for
 * fitness of purpose and serves as an exemplar of its use.
 */
final class AbstractFromApiSpec ()
	extends AnyWordSpec
		with Diagrams
		with NaturalTransformations
{
	/// Class Imports
	import io.scalaland.chimney.cats._


	/// Class Types
	final case class SampleApi (a : Int, b : String = "test")


	final case class SampleDomain (a : Int, bee : String)


	object FromApi
		extends AbstractFromApi[
			Kleisli[ErrorOr, *, *],
			ValidatedNec[String, +*],
			SampleApi,
			SampleDomain
			] ()
	{
		/// Instance Properties
		override val factory = TransformerF.define[
			ValidatedNec[String, +*],
			SampleApi,
			SampleDomain
			]
			.withFieldRenamed (_.b, _.bee)
			.buildTransformer
			.transform (_)
	}


	"The FromApi workflow" must {
		"be able to produce a 'domain' type from an 'api' instance" in {
			val api = SampleApi (a = 42)
			val result = FromApi ().run (api)

			assert (result.isRight)
			result foreach {
				domain =>
					assert (domain.a === api.a)
					assert (domain.bee === api.b)
				}
			}
		}
}

