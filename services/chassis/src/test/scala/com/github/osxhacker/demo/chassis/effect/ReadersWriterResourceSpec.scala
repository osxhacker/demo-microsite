package com.github.osxhacker.demo.chassis.effect

import cats.data.{
	Kleisli,
	StateT
	}

import cats.effect._
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AsyncWordSpec


/**
 * The '''ReadersWriterResourceSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.effect.ReadersWriterResource]] for
 * fitness of purpose and serves as an exemplar of its use.
 */
final class ReadersWriterResourceSpec ()
	extends AsyncWordSpec
		with AsyncIOSpec
		with Diagrams
{
	/// Class Imports
	import cats.syntax.applicative._
	import cats.syntax.flatMap._


	/// Class Types
	final case class ManagedInstance (
		val flag : Boolean,
		val changed : Int = 0
		)
	{
		def toggle () : ManagedInstance =
			copy (flag = !flag, changed = changed + 1)
	}


	"The ReadersWriterResource" must {
		"be able to be initialized with an existing instance" in {
			val resource = ReadersWriterResource.from[IO, ManagedInstance] (
				ManagedInstance (flag = true)
				)

			resource >>= {
				_.reader {
					instance =>
						assert (instance.flag).pure[IO]
					}
				}
			}

		"be able to be initialized with an existing managed instance" in {
			val ref = Ref[IO].of (ManagedInstance (flag = true))
			val resource = ref >>= ReadersWriterResource.fromRef[IO, ManagedInstance]

			resource >>= {
				_.reader {
					instance =>
						assert (instance.flag).pure[IO]
					}
				}
			}

		"be able to alter a managed resource" in {
			for {
				managed <- ReadersWriterResource.from[IO, ManagedInstance] (
					ManagedInstance (flag = false)
					)

				before <- managed.reader (Kleisli.ask[IO, ManagedInstance])
				altered <- managed.writer (_.toggle ().pure[IO])
				after <- managed.reader (Kleisli.ask[IO, ManagedInstance])
				} yield {
					assert (before.changed === 0)
					assert (altered.changed !== 0)
					assert (altered !== before)
					assert (altered === after)
					}
			}

		"be able to be initialized with a deferred managed instance" in {
			for {
				deferred <- Deferred[IO, Ref[IO, ManagedInstance]]
				writerTask <- ReadersWriterResource.fromDeferred (deferred)
					.flatMap {
						_.writer {
							instance =>
								assert (instance.flag === false)

								instance.toggle ().pure[IO]
						}
					}
					.start

				initial <- Ref[IO].of (ManagedInstance (flag = false))
				completed <- deferred.complete (initial)
				outcome <- writerTask.join
				latest <- deferred.get >>= (_.get)
				} yield {
					assert (completed)
					assert (outcome.isSuccess)
					assert (latest.changed === 1)
					assert (latest.flag === true)
					}
			}

		"unconditionally release locks" in {
			for {
				ref <- Ref[IO].of (ManagedInstance (flag = false))
				guarded <- ReadersWriterResource.fromRef (ref)
				readerResult <- guarded.reader {
					_ =>
						throw new RuntimeException ("a read error")
					}
					.attempt

				writerResult <- guarded.writer {
					_ =>
						throw new RuntimeException ("a write error")
					}
					.attempt

				latest <- guarded.writer (_.toggle ().pure[IO])
				} yield {
					assert (readerResult.isLeft)
					assert (writerResult.isLeft)
					assert (latest.flag === true)
					assert (latest.changed === 1)
					}
			}

		"be able to modify the resource using StateT" in {
			import StateT._

			val resource = ReadersWriterResource.from[IO, ManagedInstance] (
				ManagedInstance (flag = true)
				)

			resource >>= {
				_.writer {
					for {
						original <- get[IO, ManagedInstance]
						altered <- modify[IO, ManagedInstance] (_.toggle ())
						} yield {
							assert (original.flag)
							assert (original !== altered)
							}
					}
				}
			}
		}
}

