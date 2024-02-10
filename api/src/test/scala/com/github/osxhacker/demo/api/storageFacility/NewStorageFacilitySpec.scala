package com.github.osxhacker.demo.api.storageFacility

import org.scalatest.diagrams.Diagrams
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import com.github.osxhacker.demo.api.ProjectSpec


/**
 * The '''NewStorageFacilitySpec''' type certifies the
 * [[com.github.osxhacker.demo.api.storageFacility.NewStorageFacility]] API type
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class NewStorageFacilitySpec ()
	extends ProjectSpec
		with Diagrams
		with ScalaCheckPropertyChecks
		with StorageFacilitySupport
{
	"The NewStorageFacility API type" must {
		"be able to contain values which satisfy its contract" in {
			forAll {
				facility : NewStorageFacility =>

					assert (facility.city.value.nonEmpty)
					assert (facility.state.value.nonEmpty)
					assert (facility.zip.value.nonEmpty)
				}
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
			("." * 65)
			)

		forAll (invalidNames) {
			candidate =>
				val result = NewStorageFacility.NameType.from (candidate)

				assert (
					result.isLeft,
					s"failed to detect invalid name: '$candidate'"
					)
			}
		}

	"be able to detect and reject invalid city names" in {
		/// Valid pattern: "^[A-Za-z'][A-Za-z'-. ]*[a-z]$"
		val invalidCities = Table (
			/// Title
			"invalid city name",

			/// Leading and trailing spaces are not allowed
			" test", "\ttest", " \ttest", "\t test",
			"TEST ", "TEST\t", "TEST \t", "TEST\t ",

			/// Must be at least 2 characters long
			"A",

			/// Cannot exceed 64 characters
			("X" * 65),

			/// Cannot contain shell/HTML/programming-like characters
			"Bad & City", "Bad $ City", "Bad [ City",
			"Bad ] City", "Bad / City", "Bad % City",
			"Bad ` City", "Bad ( City", "Bad ) City"
			)

		forAll (invalidCities) {
			candidate =>
				val result = NewStorageFacility.CityType.from (candidate)

				assert (
					result.isLeft,
					s"failed to detect invalid city: '$candidate'"
					)
			}
		}

	"be able to detect and reject invalid state names" in {
		/// Valid pattern: "^[A-Z]+$"
		val invalidStates = Table (
			/// Title
			"invalid state name",

			/// Cannot be empty or spaces
			"", "  ", "   ", "\t ", " \t ", "  \t",

			/// Cannot be lower case
			"ny", "Ca", "pA"
		)

		forAll (invalidStates) {
			candidate =>
				val result = StorageFacility.StateType.from (candidate)

				assert (
					result.isLeft,
					s"failed to detect invalid state: '$candidate'"
					)
			}
		}

	"be able to detect and reject invalid zip codes" in {
		/// Valid pattern: "^[0-9]{5}(?:[0-9]{4})$"
		val invalidZips = Table (
			/// Title
			"invalid zip code",
			/// Cannot be empty or spaces
			"", "     ", "\t    ", "  \t  ", "    \t",

			/// Cannot contain non-numeric characters
			"123a5", "ABCDE", "12345-AbCdE"
			)

		forAll (invalidZips) {
			candidate =>
				val result = StorageFacility.ZipType.from (candidate)

				assert (
					result.isLeft,
					s"failed to detect invalid state: '$candidate'"
					)
			}
		}
}

