package com.github.osxhacker.demo.company.adapter.rest

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


/**
 * The '''CompanyCollectionSpec''' type defines the unit-tests which
 * certify [[com.github.osxhacker.demo.company.adapter.rest.CompanyCollection]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class CompanyCollectionSpec ()
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
	implicit private lazy val inactive = Gen.const (api.CompanyStatus.Inactive)
	private lazy val newCompany = createArbitrary[api.NewCompany] ()


	Feature ("REST company collection") {
		Scenario ("valid retrieval of all facilities when none exist") {
			implicit env =>
				Given ("the resource configured with default settings")
				val resource = CompanyCollection[IO] (defaultSettings)

				And ("a stub 'get' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.get)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val result = basicRequest
					.get (uri"$apiRoot/companies")
					.header ("X-Correlation-ID", randomUUID ().toString ())
					.send (stub)

				Then (s"the status should be ${StatusCode.Ok}")
				result map {
					response =>
						assert (response.code === StatusCode.Ok)
						assert (response.body.isRight)

						val companies = response.body
							.flatMap (decode[api.Companies])

						assert (companies.isRight)
						assert (companies.exists (_.companies.isEmpty))
					}
			}

		Scenario ("valid retrieval of all facilities") {
			implicit env =>
				Given ("the resource configured with default settings")
				val resource = CompanyCollection[IO] (defaultSettings)

				And ("a stub 'get' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.get)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val howMany = 10
				val result = addCompanies (howMany) >>
					basicRequest
						.get (uri"$apiRoot/companies")
						.header ("X-Correlation-ID", randomUUID ().toString ())
						.send (stub)

				Then (s"the status should be ${StatusCode.Ok}")
				result map {
					response =>
						assert (response.code === StatusCode.Ok)
						assert (response.body.isRight)

						val companies = response.body
							.flatMap (decode[api.Companies])

						assert (companies.isRight)
						assert (companies.exists (_.companies.size == howMany))
					}
			}

		Scenario ("create a new facility when none exist") {
			implicit env =>
				Given ("the resource configured with default settings")
				val resource = CompanyCollection[IO] (defaultSettings)

				And ("a 'new facility' under construction")
				assert (newCompany.status === api.CompanyStatus.Inactive)

				And ("a stub 'post' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.post)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated in its published location")
				val result = basicRequest
					.post (uri"$apiRoot/companies")
					.header ("X-Correlation-ID", randomUUID ().toString ())
					.body (newCompany)
					.send (stub)

				Then (s"the status should be ${StatusCode.Created}")
				result map {
					response =>
						assert (response.code === StatusCode.Created)
						assert (
							response.header ("Location").exists (_.startsWith ("/api/companies"))
							)

						assert (response.body.isRight)
						assert (response.body.exists (_.isEmpty))
					}
			}

		Scenario ("reject duplicate creation requests") {
			implicit env =>
				Given ("the resource configured with default settings")
				val resource = CompanyCollection[IO] (defaultSettings)

				And ("a stub 'post' endpoint")
				val stub = stubInterpreter[IO, Any] ()
					.whenServerEndpoint (resource.post)
					.thenRunLogic ()
					.backend ()

				When ("the endpoint is evaluated twice")
				val request = basicRequest
					.post (uri"$apiRoot/companies")
					.header ("X-Correlation-ID", randomUUID ().toString ())
					.body (newCompany)

				val result = request.send (stub) flatMap {
					first =>
						request.send (stub)
							.map (first -> _)
					}

				Then (s"the second status should not be ${StatusCode.Created}")
				result map {
					case (first, second) =>
						assert (first.code === StatusCode.Created)
						assert (!second.code.isSuccess)
						assert (first.body.isRight)
						assert (first.body.exists (_.isEmpty))
						assert (second.body.isLeft)
					}
			}
		}
}

