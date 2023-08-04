package com.github.osxhacker.demo.chassis.adapter.rest.arrow

import cats.data.{
	Kleisli,
	ValidatedNec
	}

import eu.timepit.refined
import io.scalaland.chimney.TransformerF
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec
import shapeless.tag
import shapeless.tag.@@

import com.github.osxhacker.demo.chassis.adapter.rest.{
	Path,
	ResourceLocation
	}

import com.github.osxhacker.demo.chassis.domain.{
	ErrorOr,
	NaturalTransformations
	}


/**
 * The '''AbstractToApiSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.adapter.rest.arrow.AbstractToApi]] for
 * fitness of purpose and serves as an exemplar of its use.
 */
final class AbstractToApiSpec ()
	extends AnyWordSpec
		with Diagrams
		with NaturalTransformations
{
	/// Class Imports
	import cats.syntax.applicative._
	import cats.syntax.show._
	import io.scalaland.chimney.cats._
	import refined.auto._


	/// Class Types
	final case class SampleApi (a : Int, b : String = "test")


	final case class SampleDomain (a : Int, bee : String)


	object ToApi
		extends AbstractToApi[
			Kleisli[ErrorOr, *, *],
			ValidatedNec[String, +*],
			SampleDomain,
			SampleApi
			]
	{
		/// Instance Properties
		override val addLinks = Kleisli {
			case (entity, instance, location : (ResourceLocation @@ SampleApi)) =>
				assert (entity.bee.nonEmpty)
				assert (location.show.nonEmpty)
				assert (location.show.contains ('/'))
				instance.pure[ErrorOr]
			}

		override val factory = TransformerF.define[
			ValidatedNec[String, +*],
			SampleDomain,
			SampleApi
			]
			.withFieldRenamed (_.bee, _.b)
			.buildTransformer
			.transform (_)
	}


	"The ToApi workflow" must {
		"be able to produce an 'api' instance from a 'domain' instance" in {
			val text = "from the domain"
			val domain = SampleDomain (42, text)
			val location = tag[SampleApi] (ResourceLocation (Path ("/a/b/c")))
			val result = ToApi ().run (domain -> location)

			assert (result.isRight)
			result foreach {
				api =>
					assert (api.a > 0)
					assert (api.b === text)
				}
			}
		}
}
