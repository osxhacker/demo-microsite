package com.github.osxhacker.demo.gatling

import java.net.URISyntaxException

import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec


/**
 * The '''ServiceEndpointSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.gatling.ServiceEndpoint]] for fitness of purpose
 * and serves as an exemplar of its use.
 */
final class ServiceEndpointSpec ()
	extends AnyWordSpec
		with Diagrams
{
	"A ServiceEndpoint" must {
		"be able to parse a valid 'raw' URI" in {
			val uri = "http://example.com"
			val endpoint = ServiceEndpoint (uri)

			assert (endpoint.toString === uri)
			}

		"be able to append path segments" in {
			val uri = "http://example.com"
			val root = ServiceEndpoint (uri)
			val api = root / "api"
			val underApi = root / "api" / "test"

			assert (api.toString === uri + "/api")
			assert (underApi.toString === uri + "/api/test")
			}

		"detect invalid 'raw' URI's" in {
			assertThrows[URISyntaxException] {
				ServiceEndpoint ("bad uri")
				}
			}
		}
}
