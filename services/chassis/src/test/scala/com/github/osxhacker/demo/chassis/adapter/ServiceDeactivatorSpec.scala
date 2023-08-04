package com.github.osxhacker.demo.chassis.adapter

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AsyncWordSpec


/**
 * The '''ServiceDeactivatorSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.adapter.ServiceDeactivator]] for fitness
 * of purpose and serves as an exemplar of its use.
 */
final class ServiceDeactivatorSpec ()
	extends AsyncWordSpec
		with AsyncIOSpec
		with Diagrams
{
	"The ServiceDeactivator" must {
		"support functional-style creation" in {
			ServiceDeactivator[IO] () map (_ => succeed)
			}

		"block a task until 'signal' is invoked" in {
			val message = "signalled from other task"

			for {
				deactivator <- ServiceDeactivator[IO] ()
				waiter <- deactivator.await ()
					.start

				completer <- deactivator.trySignal (message)
					.start

				result <- waiter.join
				signaller <- completer.join
				} yield {
					assert (result.isSuccess)
					assert (signaller.isSuccess)
					}
			}

		"be able to ask whether or not signal has been invoked" in {
			for {
				deactivator <- ServiceDeactivator[IO] ()
				before <- deactivator.tryGet ()
				_ <- deactivator.trySignal ("triggered")
				after <- deactivator.tryGet ()
				} yield {
					assert (before.isEmpty)
					assert (after.isDefined)
					}
			}
		}
}

