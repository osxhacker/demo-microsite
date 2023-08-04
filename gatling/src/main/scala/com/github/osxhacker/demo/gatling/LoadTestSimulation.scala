package com.github.osxhacker.demo.gatling

import scala.concurrent.duration._
import scala.language.postfixOps

import io.gatling.core.controller.inject.open.OpenInjectionStep
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.protocol.HttpProtocolBuilder


/**
 * The '''LoadTestSimulation''' type defines the common ancestor for __all__
 * [[com.github.osxhacker.demo.gatling.AbstractSimulation]]s which perform
 * load testing and __not__ end-to-end feature testing.  Where
 * [[com.github.osxhacker.demo.gatling.FeatureSimulation]] supports one or more
 * [[io.gatling.core.structure.ScenarioBuilder]]s to evaluate in sequence,
 * '''LoadTestSimulation''' supports __one__
 * [[io.gatling.core.structure.ScenarioBuilder]] which can be tuned via one or
 * more [[io.gatling.core.controller.inject.open.OpenInjectionStep]]s along with
 * a [[io.gatling.core.structure.ScenarioBuilder]] used to `initialize` the
 * system. For example:
 *
 * {{{
 *     final class MyLoadTest ()
 *         extends LoadTestSimulation ("http://example.com")
 *     {
 *          override protected val initialize = scenario (...)
 *          override protected val loadTest = scenario (...)
 *
 *
 *          evaluate (protocols.json) {
 *              nothingFor (4) ::
 *              atOnceUsers (10) ::
 *              rampUsers (10).during(5 seconds) ::
 *              Nil
 *              }
 *     }
 * }}}
 *
 * ==Knobs==
 *
 * All settings are optional.  If a setting, or any one of a required
 * combination, is not provided, then its corresponding
 * [[io.gatling.core.controller.inject.open.OpenInjectionStep]] will not be
 * available.
 *
 *   - '''simulation.burstUsers''': Number of users to simulate using the
 *     service "all at once."
 *
 *   - '''simulation.constantUsers''': Number of users to introduce per second
 *     at a __constant__ rate.
 *
 *   - '''simulation.constantUsersWindow''': Duration to use when introducing
 *     a __constant__ number of users (in seconds).
 *
 *   - '''simulation.delay''': Number of seconds to initially pause before
 *     initiating the load test.
 *
 *   - '''simulation.rampUsers''': Number of users to introduce over a period of
 *     time.
 *
 *   - '''simulation.rampUsersWindow''': Duration to use when introducing ramp
 *     users (in seconds).
 */
abstract class LoadTestSimulation (override val default : String)
	extends AbstractSimulation (default)
{
	/// Class Types
	protected object settings
	{
		/// Instance Properties
		val burstUsers = settingFor[Int] ("burstUsers").map (atOnceUsers)
			.toList

		val constant = settingFor[Int] ("constantUsers").zip (
			settingFor[Double] ("constantUsersWindow") map (_ seconds)
			)
			.map {
				case (howMany, howLong) =>
					constantUsersPerSec (howMany) during howLong
			}
			.toList

		val delay = settingFor[Double] ("delay").orElse (Some (0.5))
			.map (_ seconds)
			.map (nothingFor)
			.toList

		val ramp = settingFor[Int] ("rampUsers").zip (
			settingFor[Double] ("rampUsersWindow") map (_ seconds)
			)
			.map {
				case (howMany, howLong) =>
					rampUsers (howMany) during howLong
				}
			.toList


		private def settingFor[A] (name : String)
			(implicit numeric : Numeric[A])
			: Option[A] =
			systemProperties.get (s"simulation.$name")
				.flatMap (numeric.parseString)
	}


	/// Instance Properties
	protected def loadTest : ScenarioBuilder
	protected def initialize : ScenarioBuilder


	/**
	 * The evaluate method configures the `loadTest`` to run as a load test.
	 */
	protected def evaluate (protocol : HttpProtocolBuilder)
		(steps : Seq[OpenInjectionStep])
		: SetUp =
		setUp (
			initialize.inject (atOnceUsers (1))
				.andThen (loadTest.inject (steps))
			).protocols (protocol)
}

