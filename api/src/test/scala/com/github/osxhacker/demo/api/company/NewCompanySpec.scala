package com.github.osxhacker.demo.api.company

import org.scalatest.diagrams.Diagrams
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import com.github.osxhacker.demo.api.ProjectSpec


/**
 * The '''NewStorageFacilitySpec''' type certifies the
 * [[com.github.osxhacker.demo.api.storageFacility.NewStorageFacility]] API type
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class NewCompanySpec ()
	extends ProjectSpec
		with Diagrams
		with ScalaCheckPropertyChecks
		with CompanySupport
{
	"The NewCompany API type" must {
		"be able to contain values which satisfy its contract" in {
			forAll {
				company : NewCompany =>

					assert (company.name.value.nonEmpty)
					assert (company.slug.value.nonEmpty)
				}
			}

		"be able to detect and reject invalid names" in {
			/// Valid pattern: "^[^ \t].+[^ \t]$"
			val invalidNames = Table (
				/// Title
				"invalid name",

				/// Leading and trailing spaces are not allowed
				" test", "\ttest", " \ttest", "\t test",
				"TEST ", "TEST\t", "TEST \t", "TEST\t ",

				/// Must be at least 1 character long
				"",

				/// Cannot exceed 64 characters
				"." * 65
				)

			forAll (invalidNames) {
				candidate =>
					val result = NewCompany.NameType
						.from (candidate)

					assert (
						result.isLeft,
						s"failed to detect invalid name: '$candidate'"
						)
				}
			}

		"be able to detect and reject invalid descriptions" in {
			/// Valid pattern: "^[^ \t].+[^ \t]$"
			val invalidDescriptions = Table (
				/// Title
				"invalid description",

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

