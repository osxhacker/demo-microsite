package com.github.osxhacker.demo.gatling

import scala.sys.SystemProperties

import io.gatling.core.{
	CoreDsl,
	Predef => GatlingPredef
	}

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.filter.DenyList
import io.gatling.core.scenario.Simulation
import io.gatling.http.HttpDsl


/**
 * The '''AbstractSimulation''' type defines the common ancestor for __all__
 * [[io.gatling.core.scenario.Simulation]]s, including end-to-end feature tests
 * as well as those performing load tests.
 *
 * Each [[io.gatling.core.scenario.Simulation]] must have a `default`
 * [[https://www.rfc-editor.org/rfc/rfc3986 URI]] suitable for Gatling use.
 * Specific [[https://www.rfc-editor.org/rfc/rfc3986 URI]]s can be given by
 * setting the "service.endpoint" JVM system property.  For example:
 *
 * {{{
 *     sbt -Dservice.endpoint=http://localhost:12345 'gatling / GatlingIt / test'
 * }}}
 */
abstract class AbstractSimulation (val default : String)
	extends Simulation ()
		with CoreDsl
		with HttpDsl
		with LoggingAware
		with StatusAware
{
	/// Class Types
	protected object protocols
	{
		/// Instance Properties
		lazy val html = http.baseUrl (serviceEndpoint)
			.acceptHeader ("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
			.acceptEncodingHeader ("gzip, deflate")
			.acceptLanguageHeader ("en-US,en;q=0.5")
			.doNotTrackHeader ("1")
			.upgradeInsecureRequestsHeader ("1")
			.userAgentHeader (userAgent)

		lazy val json = http.baseUrl (serviceEndpoint)
			.acceptHeader ("application/json")
			.acceptEncodingHeader ("gzip, deflate")
			.userAgentHeader (userAgent)

		lazy val minimalHtml = html.inferHtmlResources (
			new DenyList (
				""".*\.js""" ::
				""".*\.css""" ::
				""".*\.gif""" ::
				""".*\.jpeg""" ::
				""".*\.jpg""" ::
				""".*\.ico""" ::
				""".*\.woff""" ::
				""".*\.woff2""" ::
				""".*\.(t|o)tf""" ::
				""".*\.png""" ::
				""".*\.svg""" ::
				""".*detectportal\.firefox\.com.*""" ::
				Nil
				)
			)

		private val userAgent = s"Gatling/3.9 (Simulation) ${getClass.getName}"
	}


	/// Instance Properties
	final override implicit val configuration : GatlingConfiguration =
		GatlingPredef.configuration

	final protected val serviceEndpoint : ServiceEndpoint =
		ServiceEndpoint ("service.endpoint", default)

	final protected val systemProperties = new SystemProperties ()
}

