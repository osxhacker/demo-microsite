package com.github.osxhacker.demo.chassis.adapter.rest

import scala.language.postfixOps

import eu.timepit.refined
import eu.timepit.refined.api.Refined
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec
import sttp.model.Uri

import com.github.osxhacker.demo.chassis.domain.ErrorOr


/**
 * The '''PathSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.adapter.rest.Path]] for fitness of
 * purpose and serves as an exemplar of its use.
 */
final class PathSpec ()
	extends AnyWordSpec
		with Diagrams
{
	/// Class Imports
	import cats.syntax.monoid._
	import cats.syntax.show._
	import refined.auto._
	import refined.collection.NonEmpty
	import sttp.client3._


	"The Path type" must {
		"support value type semantics" in {
			val a = Path ("/a/b/c")
			val b = Path ("/a/b/c")
			val c = Path ("/other")

			assert (a === b)
			assert (b !== c)
			assert (a.canEqual (c))
			assert (a.canEqual (99) === false)
			assert (a.hashCode () === b.hashCode ())
			assert (b.hashCode () !== c.hashCode ())
			}

		"be able to produce a parent Path" in {
			val root = Path ("/")
			val topLevel = Path ("/a")
			val multiple = Path ("/a/b/c/d")

			assert (Path.Root.parent === Path.Root)
			assert (root === Path.Root)
			assert (root.parent === Path.Root)
			assert (topLevel.parent === root)
			assert (multiple.parent === Path ("/a/b/c"))
			}

		"support pattern matching" in {
			assert (Path.unapply (Path ("/a")).isDefined)
			assert (Path.unapply ("/a").isDefined)
			assert (Path.unapply ("/a" : Refined[String, NonEmpty]).isDefined)
			assert (Path.unapply (Right ("/a")).isDefined)
			assert (Path.unapply (Left (false)).isEmpty)
			}

		"be able to parse strings" in {
			assert (Path.from[ErrorOr, String] ("/a").isRight)
			assert (Path.from[ErrorOr, String] ("not-a-path").isLeft)
			assert (Path.from[ErrorOr, String] ("").isLeft)

			/// Ensure trailing slashes are not allowed.
			assert (Path.from[ErrorOr, String] ("/a/b/").isLeft)
			}

		"be able to parse sttp Uri's" in {
			/// Ensure trailing slashes are not included.
			val candidate = uri"http://example.com/a/b/c/d/"
			val expected = "/a/b/c/d"
			val result = Path.from[ErrorOr, Uri] (candidate)

			assert (result.isRight)
			result map {
				path =>
					assert (path.show === expected)
				}
			}

		"be a model of Monoid" in {
			val a = Path ("/a")
			val leftIdentity = Path.Root |+| a
			val rightIdentity = a |+| Path.Root
			val combined = Path ("/a") |+| Path ("/b")

			assert (leftIdentity === a)
			assert (rightIdentity === a)
			assert (combined === Path ("/a/b"))
			}

		"be a model of Show" in {
			assert (Path ("/test").show === "/test")
			}
		}
}

