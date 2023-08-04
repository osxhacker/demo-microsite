package com.github.osxhacker.demo.chassis.monitoring.metrics

import cats.{
	Applicative,
	Later
	}

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import kamon.testkit.{
	InitAndStopKamonAfterAll,
	InstrumentInspection
	}

import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AsyncWordSpec


/**
 * The '''''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.monitoring.metrics.UseCaseScenario]] for
 * fitness of purpose and serves as an exemplar of its use.
 */
final class UseCaseScenarioSpec ()
	extends AsyncWordSpec
		with AsyncIOSpec
		with Diagrams
		with InitAndStopKamonAfterAll
		with InstrumentInspection.Syntax
{
	/// Class Imports
	import cats.syntax.applicative._


	/// Class Types
	final case class Sample[F[_]] (val value : String)
		(implicit private val applicative : Applicative[F])
	{
		def apply () : F[String] = value.pure[F]
	}


	"The UseCaseScenario advice" must {
		"be able to be instantiated implicitly" in {
			val advice = implicitly[UseCaseScenario[IO, Sample[IO], String]]
			val instance = Sample[IO] ("hello, world")
			val result = advice (Later (instance ()))

			assert (
				advice.called.metric.name === "app.metrics.scenario.Sample.called"
				)

			assert (
				advice.failed.metric.name === "app.metrics.scenario.Sample.failed"
				)

			assert (
				advice.succeeded.metric.name === "app.metrics.scenario.Sample.succeeded"
				)

			result.value map {
				value =>
					assert (value === instance.value)
					assert (advice.called.value (resetState = false) === 1L)
					assert (advice.failed.value (resetState = false) === 0L)
					assert (advice.succeeded.value (resetState = false) === 1L)
				}
			}
		}
}

