package com.github.osxhacker.demo.chassis.adapter.rest

import eu.timepit.refined
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec
import sttp.tapir._


/**
 * The '''ResourceLocationSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.adapter.rest.ResourceLocation]] for
 * fitness of purpose and serves as an exemplar of its use.
 */
final class ResourceLocationSpec ()
	extends AnyWordSpec
		with Diagrams
{
	/// Class Imports
	import cats.syntax.show._
	import refined.auto._


	"ResourceLocation" must {
		"be able to become a Tapir header" in {
			assertCompiles (
				"""
				endpoint.get
					.in ("test")
					.out (ResourceLocation.asHeader)
					.out (stringBody)
				"""
				)
			}

		"be able to append a subpath" in {
			val a = ResourceLocation (Path ("/first"))
			val b = a / Path ("/nested")

			assert (b.show === "/first/nested")
			}

		"support templated creation" in {
			final case class SampleParams (
				s : String,
				i : Int,
				fromHeader : Option[String]
				)


			val baseEndpoint = endpoint.get
				.in ("test" / path[String] ("s") / "segment")
				.in (path[Int] ("i"))
				.in (header[Option[String]] ("fromHeader"))
				.out (stringBody)
				.serverLogicSuccess (_ => Right ("body"))

			val params = SampleParams ("hello", 99, Some ("not in location"))
			val template = ResourceLocation.template (baseEndpoint)
			val result = template (params)

			assert (result.isRight)
			result map {
				location =>
					assert (location.show.contains (params.s))
					assert (location.show.contains (params.i.toString))
					assert (!location.show.contains (params.fromHeader.toString))
					assert (
						!location.show.contains (params.fromHeader.getOrElse ("N/A"))
						)
				}
			}
		}
}
