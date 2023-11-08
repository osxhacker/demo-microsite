package com.github.osxhacker.demo.storageFacility.adapter.rest

import java.util.UUID.randomUUID

import scala.language.postfixOps

import cats.data.Kleisli
import cats.effect.IO
import eu.timepit.refined
import io.circe
import org.scalatest.GivenWhenThen
import org.scalatest.diagrams.Diagrams
import org.scalatest.featurespec.FixtureAsyncFeatureSpecLike
import shapeless.tag
import sttp.model.StatusCode

import com.github.osxhacker.demo.chassis.adapter.rest.{
	Path,
	ResourceLocation
	}

import com.github.osxhacker.demo.chassis.domain.{
	ErrorOr,
	NaturalTransformations
	}

import com.github.osxhacker.demo.chassis.domain.entity._
import com.github.osxhacker.demo.chassis.domain.repository.CreateIntent
import com.github.osxhacker.demo.chassis.effect.ReadersWriterResource
import com.github.osxhacker.demo.storageFacility.adapter.rest.arrow._
import com.github.osxhacker.demo.storageFacility.domain._


/**
 * The '''StorageFacilityResourceSpec''' type defines the unit-tests which
 * certify
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.StorageFacilityResource]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class StorageFacilityResourceSpec ()
	extends ResourceSpec ()
		with FixtureAsyncFeatureSpecLike
		with GivenWhenThen
		with Diagrams
		with NaturalTransformations
{
	/// Class Imports
	import cats.syntax.all._
	import circe.parser.decode
	import refined.auto._
	import sttp.client3._
	import sttp.client3.circe._


	/// Class Types
	private object arrows
	{
		val toApi = FacilityToApi[ErrorOr] ()
	}


	/// Instance Properties
	private lazy val initialFacility = StorageFacility.owner
		.replace (predefined.tenant) (createArbitrary[StorageFacility] ())

	private lazy val mockResourceLocation = tag[api.StorageFacility] (
		ResourceLocation (Path ("/test"))
		)



	override def createEnvironmentResource ()
		: IO[ReadersWriterResource[IO, GlobalEnvironment[IO]]] =
		super.createEnvironmentResource ()
			.flatTap {
				_.reader (
					_.storageFacilities.save (CreateIntent (initialFacility))
					)
			}


	Feature ("storage facility resource") {
		Scenario ("retrieving an existing instance") {
			implicit env =>
				Given ("the resource configured with default settings")
				val resource = StorageFacilityResource[IO] (defaultSettings)

				And ("a stub 'get' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.get)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val result = basicRequest
					.get (
						uri"$tenantRoot/storage-facilities/${initialFacility.id.toUuid ()}?expand=all"
						)
					.header ("X-Correlation-ID", randomUUID ().toString ())
					.send (stub)

				Then (s"the status should be ${StatusCode.Ok}")
				result map {
					response =>
						assert (response.code === StatusCode.Ok)
						assert (response.body.isRight)

						val facility = response.body
							.leftMap (new RuntimeException (_))
							.flatMap (decode[api.StorageFacility])

						val returnedVersion = facility.map (_.version)
							.flatMap (v => Version[ErrorOr] (v.value))

						assert (facility.isRight)
						assert (
							facility.exists (_.id.value == initialFacility.id.toUrn ())
							)

						assert (facility.exists (_._embedded.isDefined))
						assert (
							facility.exists (_._embedded.exists (_.values.contains ("company")))
							)

						assert (
							returnedVersion.exists (_ >= initialFacility.version)
							)
				}
			}

		Scenario ("gracefully reject retrieving non-existent instance") {
			implicit env =>
				Given ("the resource configured with default settings")
				val resource = StorageFacilityResource[IO] (defaultSettings)

				And ("a stub 'get' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.get)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val result = basicRequest
					.get (
						uri"$apiRoot/test-company/storage-facilities/${randomUUID ()}"
						)
					.header ("X-Correlation-ID", randomUUID ().toString ())
					.send (stub)

				Then ("the request should not succeed")
				result map {
					response =>
						/// Note that the Tapir stub mechanism does not run
						/// errors through the same flow as when endpoints are
						/// evaluated within a server.  Therefore, only check to
						/// see if the response is not a 2xx.
						assert (!response.isSuccess)
					}
			}

		Scenario ("removing an existing instance") {
			implicit env =>
				Given ("the resource configured with default settings")
				val resource = StorageFacilityResource[IO] (defaultSettings)

				And ("a stub 'delete' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.delete)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val result = basicRequest
					.delete (
						uri"$tenantRoot/storage-facilities/${initialFacility.id.toUuid ()}"
						)
					.header ("X-Correlation-ID", randomUUID ().toString ())
					.send (stub)

				Then (s"the status should be ${StatusCode.Ok}")
				result map {
					response =>
						assert (response.code === StatusCode.Ok)
					}
			}

		Scenario ("gracefully reject removing non-existent instance") {
			implicit env =>
				Given ("the resource configured with default settings")
				val resource = StorageFacilityResource[IO] (defaultSettings)

				And ("a stub 'delete' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.delete)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val result = basicRequest
					.delete (
						uri"$apiRoot/test-company/storage-facilities/${randomUUID ()}"
						)
					.header ("X-Correlation-ID", randomUUID ().toString ())
					.send (stub)

				Then ("the request should not succeed")
				result map {
					response =>
						/// Note that the Tapir stub mechanism does not run
						/// errors through the same flow as when endpoints are
						/// evaluated within a server.  Therefore, only check to
						/// see if the response is not a 2xx.
						assert (!response.isSuccess)
					}
			}

		Scenario ("altering an existing instance") {
			implicit env =>
				Given ("the resource configured with default settings")
				val resource = StorageFacilityResource[IO] (defaultSettings)

				And ("a stub 'post' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.post)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val result = basicRequest
					.post (
						uri"$tenantRoot/storage-facilities/${initialFacility.id.toUuid ()}?expand=all"
						)
					.header ("X-Correlation-ID", randomUUID ().toString ())
					.body {
						Kleisli.liftF (initialFacility.touch[ErrorOr] ())
							.flatMapF {
								sf =>
									arrows.toApi ().run (
										sf -> mockResourceLocation
										)
								}
							.run (initialFacility)
							.orFail ()
						}
					.send (stub)

				Then (s"the request should be ${StatusCode.Ok}")
				result map {
					response =>
						assert (response.code === StatusCode.Ok)
						assert (response.body.isRight)

						val facility = response.body
							.leftMap (new RuntimeException (_))
							.flatMap (decode[api.StorageFacility])

						val returnedVersion = facility.map (_.version)
							.flatMap (v => Version[ErrorOr] (v.value))

						assert (facility.isRight)
						assert (
							facility.exists (_.id.value == initialFacility.id.toUrn ())
							)

						assert (
							facility.exists (_._embedded.exists (_.values.contains ("company")))
							)

						assert (
							returnedVersion.exists (_ >= initialFacility.version)
							)
					}
			}

		Scenario ("gracefully reject altering with an invalid 'expand'") {
			implicit env =>
				Given ("the resource configured with default settings")
				val resource = StorageFacilityResource[IO] (defaultSettings)

				And ("a stub 'post' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.post)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val result = basicRequest
					.post (
						uri"$tenantRoot/storage-facilities/${initialFacility.id.toUuid ()}?expand=unknown-expansion"
						)
					.header ("X-Correlation-ID", randomUUID ().toString ())
					.body {
						Kleisli.liftF (initialFacility.touch[ErrorOr] ())
							.flatMapF {
								sf =>
									arrows.toApi ().run (
										sf -> mockResourceLocation
										)
								}
							.run (initialFacility)
							.orFail ()
						}
					.send (stub)

				Then ("the request should not succeed")
				result map {
					response =>
						/// Note that the Tapir stub mechanism does not run
						/// errors through the same flow as when endpoints are
						/// evaluated within a server.  Therefore, only check to
						/// see if the response is not a 2xx.
						assert (!response.isSuccess)
					}
			}

		Scenario ("gracefully reject altering non-existent instance") {
			implicit env =>
				Given ("the resource configured with default settings")
				val resource = StorageFacilityResource[IO] (defaultSettings)

				And ("a stub 'post' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.post)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val result = basicRequest
					.post (
						uri"$apiRoot/test-company/storage-facilities/${randomUUID ()}"
						)
					.header ("X-Correlation-ID", randomUUID ().toString ())
					.body (
						arrows.toApi ()
							.run (initialFacility -> mockResourceLocation)
							.orFail ()
						)
					.send (stub)

				Then ("the request should not succeed")
				result map {
					response =>
						/// Note that the Tapir stub mechanism does not run
						/// errors through the same flow as when endpoints are
						/// evaluated within a server.  Therefore, only check to
						/// see if the response is not a 2xx.
						assert (!response.isSuccess)
					}
			}

		Scenario ("close an active facility") {
			implicit env =>
				Given ("the resource configured with default settings")
				val resource = StorageFacilityResource[IO] (defaultSettings)

				And ("a stub 'post' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.close)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val result = basicRequest
					.post (
						uri"$tenantRoot/storage-facilities/${initialFacility.id.toUuid ()}/close"
						)
					.header ("X-Correlation-ID", randomUUID ().toString ())
					.body {
						initialFacility.touch[ErrorOr] ()
							.map {
								StorageFacility.version
									.andThen (Version.value)
									.get (_)
									.value
								}
							.flatMap (api.VersionOnly.from)
							.orFail ()
						}
					.send (stub)

				Then (s"the request should be ${StatusCode.Ok}")
				result map {
					response =>
						assert (response.code === StatusCode.Ok)
						assert (response.body.isRight)

						val facility = response.body
							.leftMap (new RuntimeException (_))
							.flatMap (decode[api.StorageFacility])

						val returnedVersion = facility.map (_.version)
							.flatMap (v => Version[ErrorOr] (v.value))

						assert (facility.isRight)
						assert (
							facility.exists (_.id.value == initialFacility.id.toUrn ())
						)

						assert (
							returnedVersion.exists (_ >= initialFacility.version)
						)
				}
			}
		}
}

