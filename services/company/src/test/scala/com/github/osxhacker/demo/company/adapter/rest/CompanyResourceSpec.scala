package com.github.osxhacker.demo.company.adapter.rest

import java.util.UUID.randomUUID

import scala.language.postfixOps

import cats.effect.IO
import eu.timepit.refined
import io.circe
import kamon.testkit.InitAndStopKamonAfterAll
import org.scalatest.GivenWhenThen
import org.scalatest.diagrams.Diagrams
import org.scalatest.featurespec.FixtureAsyncFeatureSpecLike
import shapeless.tag
import shapeless.tag.@@
import sttp.model.StatusCode

import com.github.osxhacker.demo.chassis.adapter.rest.{
	Path,
	ResourceLocation
	}

import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.chassis.domain.entity._
import com.github.osxhacker.demo.chassis.domain.repository.CreateIntent
import com.github.osxhacker.demo.chassis.effect.ReadersWriterResource
import com.github.osxhacker.demo.company.domain._
import com.github.osxhacker.demo.company.domain.specification.CompanyStatusIs


/**
 * The '''CompanyResourceSpec''' type defines the unit-tests which
 * certify [[com.github.osxhacker.demo.company.adapter.rest.CompanyResource]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class CompanyResourceSpec ()
	extends ResourceSpec ()
		with FixtureAsyncFeatureSpecLike
		with GivenWhenThen
		with Diagrams
		with InitAndStopKamonAfterAll
{
	/// Class Imports
	import arrow._
	import cats.syntax.all._
	import circe.parser.decode
	import refined.auto._
	import sttp.client3._
	import sttp.client3.circe._


	/// Class Types
	private object arrows
	{
		val toApi = CompanyToApi[ErrorOr] ()
	}


	/// Instance Properties
	private lazy val initialCompany = createArbitrary[Company] ()
	private lazy val mockResourceLocation : ResourceLocation @@ api.Company =
		tag[api.Company] (ResourceLocation (Path ("/test")))


	override def createEnvironmentResource ()
		: IO[ReadersWriterResource[IO, GlobalEnvironment[IO]]] =
		super.createEnvironmentResource ()
			.flatTap {
				_.reader (
					_.companies
						.save (CreateIntent (initialCompany))
					)
				}


	Feature ("company resource") {
		Scenario ("retrieving an existing instance") {
			implicit env =>
				Given ("the resource configured with default settings")
				val resource = CompanyResource[IO] (defaultSettings)

				assert (CompanyStatusIs (CompanyStatus.Active) (initialCompany))

				And ("a stub 'get' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.get)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val result = basicRequest
					.get (
						uri"$apiRoot/companies/${initialCompany.id.toUuid ()}"
						)
					.header ("X-Correlation-ID", randomUUID ().toString ())
					.send (stub)

				Then (s"the status should be ${StatusCode.Ok}")
				result map {
					response =>
						assert (response.code === StatusCode.Ok)
						assert (response.body.isRight)

						val company = response.body
							.leftMap (new RuntimeException (_))
							.flatMap (decode[api.Company])

						val returnedVersion = company.map (_.version)
							.flatMap (v => Version[ErrorOr] (v.value))

						val links = company.map (_._links.flatMap (_.additionalProperties))

						assert (company.isRight)
						assert (
							company.exists (_.id.value == initialCompany.id.toUrn ())
							)

						assert (
							returnedVersion.exists (_ >= initialCompany.version)
							)

						assert (links.exists (_.nonEmpty))
						assert (links.exists (_.exists (_.nonEmpty)))

						Set (
							"urn:company:deactivate",
							"urn:company:suspend"
							) foreach {
							relation =>
								assert (
									links.exists (_.exists (_.keySet.contains (relation)))
									)
							}
					}
			}

		Scenario ("gracefully reject retrieving non-existent instance") {
			implicit env =>
				Given ("the resource configured with default settings")
				val resource = CompanyResource[IO] (defaultSettings)

				And ("a stub 'get' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.get)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val result = basicRequest
					.get (
						uri"$apiRoot/companies/${randomUUID ()}"
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
				val resource = CompanyResource[IO] (defaultSettings)

				And ("a stub 'delete' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.delete)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val result = basicRequest
					.delete (
						uri"$apiRoot/companies/${initialCompany.id.toUuid ()}"
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
				val resource = CompanyResource[IO] (defaultSettings)

				And ("a stub 'delete' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.delete)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val result = basicRequest
					.delete (
						uri"$apiRoot/companies/${randomUUID ()}"
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
				val resource = CompanyResource[IO] (defaultSettings)

				And ("a stub 'post' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.post)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val result = basicRequest
					.post (
						uri"$apiRoot/companies/${initialCompany.id.toUuid ()}"
						)
					.header ("X-Correlation-ID", randomUUID ().toString ())
					.body {
						val altered = initialCompany.changeStatusTo[ErrorOr] (
							CompanyStatus.Suspended
							)
							.orFail ()

						arrows.toApi ()
							.run (altered -> mockResourceLocation)
							.orFail ()
						}
					.send (stub)

				Then (s"the request should be ${StatusCode.Ok}")
				result map {
					response =>
						assert (response.code === StatusCode.Ok)
						assert (response.body.isRight)

						val company = response.body
							.leftMap (new RuntimeException (_))
							.flatMap (decode[api.Company])

						val returnedVersion = company.map (_.version)
							.flatMap (v => Version[ErrorOr] (v.value))

						assert (company.isRight)
						assert (
							company.exists (_.id.value == initialCompany.id.toUrn ())
							)

						assert (
							returnedVersion.exists (_ >= initialCompany.version)
							)
					}
			}

		Scenario ("gracefully reject altering non-existent instance") {
			implicit env =>
				Given ("the resource configured with default settings")
				val resource = CompanyResource[IO] (defaultSettings)

				And ("a stub 'post' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.post)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val result = basicRequest
					.post (uri"$apiRoot/companies/${randomUUID ()}")
					.header ("X-Correlation-ID", randomUUID ().toString ())
					.body (
						arrows.toApi ()
							.run (initialCompany -> mockResourceLocation)
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
		}
}

