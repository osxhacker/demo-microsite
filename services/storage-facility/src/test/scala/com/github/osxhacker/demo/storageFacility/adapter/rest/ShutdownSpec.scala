package com.github.osxhacker.demo.storageFacility.adapter.rest

import java.util.UUID.randomUUID

import cats.effect.IO
import org.scalatest.GivenWhenThen
import org.scalatest.diagrams.Diagrams
import org.scalatest.featurespec.FixtureAsyncFeatureSpecLike
import sttp.client3._
import sttp.client3.circe._
import sttp.model.StatusCode

import com.github.osxhacker.demo.storageFacility.adapter.rest.api._
import com.github.osxhacker.demo.chassis.adapter.ServiceDeactivator


/**
 * The '''ShutdownSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.Shutdown]] for
 * fitness of purpose and serves as an exemplar of its use.
 */
final class ShutdownSpec ()
	extends ResourceSpec ()
		with FixtureAsyncFeatureSpecLike
		with GivenWhenThen
		with Diagrams
{
	Feature ("shutdown the service") {
		Scenario ("invoking an initial shutdown") {
			implicit env =>
				Given ("the resource configured with default settings")
				val signal = halt ()
				val resource = Shutdown[IO] (defaultSettings, signal)

				And ("a stub 'put' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.put)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val message = ShutdownMessage.from ("simulated shutdown")
					.orFail ()

				val result = basicRequest
					.put (uri"$internalRoot/shutdown")
					.header ("X-Correlation-ID", randomUUID ().toString ())
					.header ("User-Agent", "integration-test")
					.body (message)
					.send (stub)

				Then (s"the status should be ${StatusCode.Ok}")
				sequence (result, signal.tryGet ()) map {
					case (response, halted) =>
						assert (response.code === StatusCode.Ok)
						assert (halted.isDefined)
					}
			}

		Scenario ("attempting to shut down without a message") {
			implicit env =>
				Given ("the resource configured with default settings")
				val signal = halt ()
				val resource = Shutdown[IO] (defaultSettings, signal)

				And ("a stub 'put' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.put)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val result = basicRequest
					.put (uri"$internalRoot/shutdown")
					.contentType ("application/json")
					.header ("X-Correlation-ID", randomUUID ().toString ())
					.header ("User-Agent", "integration-test")
					.send (stub)

				Then (s"the status should be a client error")
				sequence (result, signal.tryGet ()) map {
					case (response, halted) =>
						assert (response.isClientError)
						assert (halted.isEmpty)
					}
			}

		Scenario ("attempting to shut down twice") {
			implicit env =>
				Given ("the resource configured with default settings")
				val resource = Shutdown[IO] (defaultSettings, halt ())

				And ("a stub 'put' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.put)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val message = ShutdownMessage.from ("simulated shutdown")
					.orFail ()

				val first = basicRequest
					.put (uri"$internalRoot/shutdown")
					.header ("X-Correlation-ID", randomUUID ().toString ())
					.header ("User-Agent", "integration-test")
					.body (message)
					.send (stub)

				And ("the endpoint is evaluated again")
				val second = first >> basicRequest
					.put (uri"$internalRoot/shutdown")
					.header ("X-Correlation-ID", randomUUID ().toString ())
					.header ("User-Agent", "integration-test")
					.body (message)
					.send (stub)

				Then ("the first should succeed and second fail")
				sequence (first, second) map {
					case (f, s) =>
						assert (f.isSuccess)
						assert (s.isClientError)
					}
			}
		}


	private def halt () = ServiceDeactivator.unsafe[IO] ()
}

