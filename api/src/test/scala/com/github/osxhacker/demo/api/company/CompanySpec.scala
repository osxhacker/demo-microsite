package com.github.osxhacker.demo.api.company

import eu.timepit.refined
import io.circe.parser
import org.scalatest.diagrams.Diagrams
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import com.github.osxhacker.demo.api.ProjectSpec


/**
 * The '''CompanySpec''' type certifies the
 * [[com.github.osxhacker.demo.api.company.Company]] API type for fitness of
 * purpose and serves as an exemplar of its use.
 */
final class CompanySpec ()
	extends ProjectSpec
		with Diagrams
		with ScalaCheckPropertyChecks
		with CompanySupport
{
	/// Class Imports
	import cats.syntax.eq._
	import cats.syntax.option._
	import io.circe.syntax._
	import refined.auto._


	"The StorageFacility API type" must {
		"be a model of ResourceObject" in {
			val parentType = classOf[ResourceObject]

			assert (parentType.isAssignableFrom (classOf[Company]))
			}

		"be able to contain values which satisfy its contract" in {
			forAll {
				company : Company =>

					assert (company._links.isEmpty)
					assert (company._embedded.isEmpty)
					assert (company.name.value.nonEmpty)
				}
			}

		"be able to 'round trip' with semantics links" in {
			forAll {
				company : Company =>
					val withSemantics = Company.Optics._links
						.replace (
							Links () (
								Links.AdditionalProperties (
									Map (
										"self" -> LinkObject (
											SupportedHttpMethods.GET,
											"http://example.com/test",
											"application/json"
											).asJson
										)
									).some
								).some
							) (company)

					val encoded = withSemantics.asJson
						.toString

					val decoded = parser.decode[Company] (encoded)

					assert (withSemantics._links.isDefined)
					assert (encoded.nonEmpty)
					assert (decoded.isRight)
					assert (decoded.exists (_ == withSemantics))
					assert (decoded.exists (_._links == withSemantics._links))
				}
			}

		"allow valid slugs" in {
			/// Valid pattern: "^[a-z](?:[a-z0-9]+|(?:[a-z0-9]*(?:-[a-z0-9]+)+))$"
			val validSlugs = Table (
				"a-valid-slug",
				"a-0-valid-slug",
				"a-0valid-slug",
				"a-valid0-slug",
				"a-0valid0-slug",
				"a0-valid-slug",
				"a0-0valid-slug",
				"a0-valid0-slug",
				"a0-0valid0-slug",
				"bus-4-us"
				)

			forAll (validSlugs) {
				candidate =>
					val result = Company.SlugType.from (candidate)

					assert (
						result.isRight,
						s"failed to allow valid slug: '$candidate'"
						)
				}
			}

		"be able to detect and reject invalid slugs" in {
			/// Valid pattern: "^[a-z](?:[a-z0-9]+|(?:[a-z0-9]*(?:-[a-z0-9]+)+))$"
			val invalidSlugs = Table (
				/// Leading and trailing spaces are not allowed
				" test", "\ttest", " \ttest", "\t test",
				"TEST ", "TEST\t", "TEST \t", "TEST\t ",

				/// Single word definitions must have at least two characters
				"x", "0",

				/// Definition words cannot start with digits
				"0abc", "99bad-multi-word"
				)

			forAll (invalidSlugs) {
				candidate =>
					val result = Company.SlugType.from (candidate)

					assert (
						result.isLeft,
						s"failed to detect invalid slug: '$candidate'"
						)
				}
			}

		"be able to detect and reject invalid names" in {
			/// Valid pattern: "^[^ \t].+[^ \t]$"
			val invalidNames = Table (
				/// Leading and trailing spaces are not allowed
				" test", "\ttest", " \ttest", "\t test",
				"TEST ", "TEST\t", "TEST \t", "TEST\t ",

				/// Must be at least 2 characters long
				"", "x",

				/// Cannot exceed 64 characters
				"." * 65
				)

			forAll (invalidNames) {
				candidate =>
					val result = Company.NameType.from (candidate)

					assert (
						result.isLeft,
						s"failed to detect invalid name: '$candidate'"
						)
					}
			}

		"be able to detect and reject invalid descriptions" in {
			/// Valid pattern: "^[^ \t].+[^ \t]$"
			val invalidDescriptions = Table (
				/// Leading and trailing spaces are not allowed
				" test", "\ttest", " \ttest", "\t test",
				"TEST ", "TEST\t", "TEST \t", "TEST\t ",

				/// Cannot exceed 2048 characters
				"." * 2049
			)

			forAll (invalidDescriptions) {
				candidate =>
					val result = NewCompany.DescriptionType
						.from (candidate)

					assert (
						result.isLeft,
						s"failed to detect invalid description: '$candidate'"
						)
				}
			}
		}
}

