package com.github.osxhacker.demo.chassis.effect

import cats.Later
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.diagrams.Diagrams
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
		with Diagrams
{
	/// Class Imports
	import cats.syntax.applicative._


	"The Pointcut support for IO" must {
		"be able to invoke logic 'before'" in {
			var boolean = false
			val result = Pointcut[IO] ().before {
				Later {
					assert (boolean === true)
					1.pure[IO]
					}
				} (
				() => {
					assert (boolean === false)
					boolean = true
					}
				)
				.value

			result map {
				_ =>
					assert (boolean === true)
				}
			}

		"be able to invoke logic 'after'" in {
			var int = 0
			val result = Pointcut[IO] ().after {
				Later {
					assert (int === 0)
					1.pure[IO]
					}
				} (
				a => {
					assert (int === 0)
					int = a
					}
				)
				.value

			result map {
				value =>
					assert (int === value)
				}
			}

		"be able to invoke logic 'around'" in {
			val sequence = new StringBuilder ()
			val result = Pointcut[IO] ().around (
				Later {
					assert (sequence.toString ().nonEmpty)
					1.pure[IO]
					}
				) (
				entering = () => sequence.append ("entering, "),
				leaving = sequence.append ("leaving: ").append (_),
				onError = _ => sequence.append (" error!")
				)
				.value

			result map {
				value =>
					assert (value === 1)
					assert (sequence.toString () === "entering, leaving: 1")
				}
			}

		"be able to detect errors" in {
			val sequence = new StringBuilder ()
			val result = Pointcut[IO] ().always[Int] (
				Later {
					assert (sequence.toString ().isEmpty)
					IO.raiseError (new Exception ("boom!"))
					}
				) (
					leaving = _ => sequence.append ("leaving,"),
					onError = _ => sequence.append ("errored!")
				)
				.value

			result.flatMap (_ => fail ("expected an error condition"))
				.recover {
				_ =>
					assert (sequence.toString () === "errored!")
				}
			}
		}
}

