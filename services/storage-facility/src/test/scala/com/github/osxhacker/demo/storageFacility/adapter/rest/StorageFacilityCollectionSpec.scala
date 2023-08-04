package com.github.osxhacker.demo.storageFacility.adapter.rest

import java.util.UUID.randomUUID

import scala.language.postfixOps

import cats.effect.IO
import io.circe
import kamon.testkit.InitAndStopKamonAfterAll
import org.scalacheck.Gen
import org.scalatest.GivenWhenThen
import org.scalatest.diagrams.Diagrams
import org.scalatest.featurespec.FixtureAsyncFeatureSpecLike
import sttp.model.StatusCode

import com.github.osxhacker.demo.storageFacility.domain.Company


/**
 * The '''StorageFacilityCollectionSpec ''' type defines the unit-tests which
 * certify
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.StorageFacilityCollection]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class StorageFacilityCollectionSpec ()
	extends ResourceSpec ()
		with FixtureAsyncFeatureSpecLike
		with GivenWhenThen
		with Diagrams
		with InitAndStopKamonAfterAll
{
	/// Class Imports
	import circe.parser.decode
	import sttp.client3._
	import sttp.client3.circe._


	/// Instance Properties
	implicit private lazy val underConstruction =
		Gen.const (api.StorageFacilityStatus.UnderConstruction)

	private lazy val newFacility = createArbitrary[api.NewStorageFacility] ()


	Feature ("storage facility collection") {
		Scenario ("valid retrieval of all facilities when none exist") {
			implicit env =>
				Given ("the resource configured with default settings")
				val resource = StorageFacilityCollection[IO] (defaultSettings)

				And ("a stub 'get' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.get)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val result = basicRequest
					.get (uri"$tenantRoot/storage-facilities?expand=company")
					.header ("X-Correlation-ID", randomUUID ().toString ())
					.send (stub)

				Then (s"the status should be ${StatusCode.Ok}")
				result map {
					response =>
						assert (response.code === StatusCode.Ok)
						assert (response.body.isRight)

						val facilities = response.body
							.flatMap (decode[api.StorageFacilities])

						assert (facilities.isRight)
						assert (facilities.exists (_.facilities.isEmpty))
						assert (facilities.exists (_._embedded.isDefined))
						assert (
							facilities.exists (_._embedded.exists (_.values.contains ("company")))
							)
					}
			}

		Scenario ("valid retrieval of all facilities for a tenant") {
			implicit env =>
				Given ("the resource configured with default settings")
				val resource = StorageFacilityCollection[IO] (defaultSettings)

				And ("a stub 'get' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.get)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val howMany = 10
				val result = addFacilities (howMany, predefined.tenant) >>
					basicRequest
						.get (uri"$tenantRoot/storage-facilities")
						.header ("X-Correlation-ID", randomUUID ().toString ())
						.send (stub)

				Then (s"the status should be ${StatusCode.Ok}")
				result map {
					response =>
						assert (response.code === StatusCode.Ok)
						assert (response.body.isRight)

						val facilities = response.body
							.flatMap (decode[api.StorageFacilities])

						And (s"there should be $howMany facilities returned")
						assert (facilities.isRight)
						assert (facilities.exists (_.facilities.size == howMany))
						assert (facilities.exists (_._embedded.isEmpty))
					}
			}

		Scenario ("does not return facilities owned by another tenant") {
			implicit env =>
				Given ("the resource configured with default settings")
				val resource = StorageFacilityCollection[IO] (defaultSettings)

				And ("a stub 'get' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.get)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val howMany = 10
				val result = addFacilities (howMany, createArbitrary[Company] ()) >>
					basicRequest
						.get (uri"$tenantRoot/storage-facilities")
						.header ("X-Correlation-ID", randomUUID ().toString ())
						.send (stub)

				Then (s"the status should be ${StatusCode.Ok}")
				result map {
					response =>
						assert (response.code === StatusCode.Ok)
						assert (response.body.isRight)

						val facilities = response.body
							.flatMap (decode[api.StorageFacilities])

						And ("there should be no facilities returned")
						assert (facilities.isRight)
						assert (facilities.exists (_.facilities.isEmpty))
					}
		}

		Scenario ("create a new facility when none exist") {
			implicit env =>
				Given ("the resource configured with default settings")
				val resource = StorageFacilityCollection[IO] (defaultSettings)

				And ("a 'new facility' under construction")
				assert (
					newFacility.status === api.StorageFacilityStatus.UnderConstruction
					)

				And ("a stub 'put' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.put)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val result = basicRequest
					.put (uri"$tenantRoot/storage-facilities")
					.header ("X-Correlation-ID", randomUUID ().toString ())
					.body (newFacility)
					.send (stub)

				Then (s"the status should be ${StatusCode.Created}")
				result map {
					response =>
						val location = response.header ("Location")

						assert (response.code === StatusCode.Created)
						assert (location.isDefined)
						assert (
							location.exists (
								_.startsWith ("/")
								),

							s"expected location to be an absolute path, got: $location"
							)

						assert (
							location.exists (
								_.startsWith (tenantRoot.path.mkString ("/", "/", ""))
								)
							)

						assert (response.body.isRight)
						assert (response.body.exists (_.isEmpty))
					}
			}

		Scenario ("support duplicate creation requests") {
			implicit env =>
				Given ("the resource configured with default settings")
				val resource = StorageFacilityCollection[IO] (defaultSettings)

				And ("a stub 'put' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.put)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated twice")
				val request = basicRequest
					.put (uri"$tenantRoot/storage-facilities")
					.header ("X-Correlation-ID", randomUUID ().toString ())
					.body (newFacility)

				val result = request.send (stub) flatMap {
					first =>
						request.send (stub)
							.map (first -> _)
					}

				Then (s"the status should be ${StatusCode.Created}")
				result map {
					case (first, second) =>
						assert (first.code === StatusCode.Created)
						assert (
							first.header ("Location") == second.header ("Location")
							)

						assert (first.body.isRight)
						assert (first.body.exists (_.isEmpty))
						assert (second.body.isRight)
						assert (second.body.exists (_.isEmpty))
					}
			}
		}
}

