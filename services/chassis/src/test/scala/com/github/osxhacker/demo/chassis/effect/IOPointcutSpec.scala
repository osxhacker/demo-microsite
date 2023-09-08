package com.github.osxhacker.demo.chassis.effect

import scala.language.postfixOps

import cats.Now
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.wordspec.AsyncWordSpec


/**
 * The '''IOPointcutSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.effect.Pointcut]] for fitness of purpose
 * and serves as an exemplar of its use.
 *
 * @see [[com.github.osxhacker.demo.chassis.effect.PointcutSpec]]
 */
final class IOPointcutSpec ()
	extends AsyncWordSpec
		with AsyncIOSpec
		with PointcutBehaviours
{
	/// Class Imports
	import cats.syntax.either._


	"The Pointcut type class" when {
		"bound to IO" must {
			"not immediately evaluate the functor given to 'before'" in {
				@volatile
				var called = false
				val result = Pointcut[IO] ().before (Now (IO ("the value"))) {
					() => called = true
					}

				/// The before functor should not be called during construction
				/// of the Eval[IO[String]].
				assert (called === false)

				val operations = result value

				/// The before functor should not be called during resolution
				/// of the IO[String].
				assert (called === false)
				operations map {
					s =>
						/// This is when the before functor is expected to have
						/// been evaluated by `Pointcut[IO]`.
						assert (called === true)
						assert (s nonEmpty)
					}
				}

			"not immediately evaluate functors given to 'bracket'" in {
				@volatile
				var acquired = false

				@volatile
				var released = false

				val result = Pointcut[IO] ().bracket (Now (IO (99))) (
					() => (acquired = true).asRight[Throwable]
					) {
					_ => errorOrInt =>
						errorOrInt.foreach (_ => released = true)
					}

				/// The acquire and release functors should not be called during
				/// construction of the Eval[IO[Int]].
				assert (acquired === false)
				assert (released === false)

				val operations = result value

				/// The acquire and release functors should not be called during
				/// resolution of the IO[Int].
				assert (acquired === false)
				assert (released === false)

				operations map {
					i =>
						/// This is when functors are expected to have been
						/// evaluated by `Pointcut[IO]`.
						assert (acquired === true)
						assert (released === true)
						assert (i === 99)
					}
			}

			behave like functionalAOP[IO] ()
			}
		}
}

