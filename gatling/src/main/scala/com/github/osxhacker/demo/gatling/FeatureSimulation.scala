package com.github.osxhacker.demo.gatling

import scala.language.postfixOps

import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.protocol.HttpProtocolBuilder


/**
 * The '''FeatureSimulation''' type defines the common ancestor for __all__
 * [[com.github.osxhacker.demo.gatling.AbstractSimulation]]s which perform
 * end-to-end feature testing and __not__ load testing.
 */
abstract class FeatureSimulation (override val default : String)
	extends AbstractSimulation (default)
{
	/// Instance Properties
	private val maximumUsers = atOnceUsers (1)


	/**
	 * The evaluate method configures the given '''scenarios''' to run as
	 * end-to-end feature interactions.
	 */
	protected def evaluate (protocol : HttpProtocolBuilder)
		(scenarios : List[ScenarioBuilder])
		: SetUp =
		setUp (
			scenarios.tail.foldLeft (scenarios.head.inject (maximumUsers)) {
				(accum, next) =>
					accum andThen next.inject (maximumUsers)
				}
			).protocols (protocol)
}

