package com.github.osxhacker.demo.company.adapter.rest

import java.util.UUID.randomUUID

import cats.effect.IO
import org.scalatest.GivenWhenThen
import org.scalatest.diagrams.Diagrams
import org.scalatest.featurespec.FixtureAsyncFeatureSpecLike
import sttp.client3._
import sttp.model.StatusCode


/**
 * The '''HeartbeatSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.company.adapter.rest.Heartbeat]] for
 * fitness of purpose and serves as an exemplar of its use.
 */
final class HeartbeatSpec ()
	extends ResourceSpec ()
		with FixtureAsyncFeatureSpecLike
		with GivenWhenThen
		with Diagrams
{
	/// Class Imports
	import cats.syntax.applicative._


	Feature ("service heartbeat") {
		Scenario ("valid use of the Heartbeat resource") {
			implicit env =>
				Given ("the resource configured with default settings")
				val resource = Heartbeat[IO] (defaultSettings)

				And ("a stub 'get' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.get)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val result = basicRequest
					.get (uri"$internalRoot/heartbeat")
					.header ("X-Correlation-ID", randomUUID ().toString ())
					.send (stub)

				Then (s"the status should be ${StatusCode.Ok}")
				result map {
					response =>
						assert (response.code === StatusCode.Ok)
					}
			}

		Scenario ("ensure heartbeat is not exposed under the api root") {
			implicit env =>
				Given ("the resource configured with default settings")
				val resource = Heartbeat[IO] (defaultSettings)

				And ("a stub 'get' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.get)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated as an api location")
				val result = basicRequest
					.get (uri"$apiRoot/heartbeat")
					.header ("X-Correlation-ID", randomUUID ().toString ())
					.send (stub)

				Then (s"the status should be ${StatusCode.NotFound}")
				result map {
					response =>
						assert (response.code === StatusCode.NotFound)
				}
			}

		Scenario ("short-circuit evaluation when offline") {
			implicit env =>
				Given ("the resource configured with default settings")
				val resource = Heartbeat[IO] (defaultSettings)

				And ("a stub 'get' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.get)
					.thenRunLogic ()
					.backend ()

				When ("the environment is made offline then endpoint evaluated")
				val result =
					env.writer (_.quiesce ("going offline").pure[IO]) >>
						basicRequest.get (uri"$internalRoot/heartbeat")
							.header ("X-Correlation-ID", randomUUID ().toString ())
							.send (stub)

				Then (s"the status should be ${StatusCode.ServiceUnavailable}")
				result map {
					response =>
						assert (response.code === StatusCode.ServiceUnavailable)
				}
			}
		}
}

