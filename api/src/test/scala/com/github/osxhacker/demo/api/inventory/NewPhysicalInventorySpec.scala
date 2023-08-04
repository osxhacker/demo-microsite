package com.github.osxhacker.demo.api.inventory

import org.scalacheck.Gen
import org.scalatest.diagrams.Diagrams
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import com.github.osxhacker.demo.api.ProjectSpec


/**
 * The '''NewPhysicalInventorySpec''' type certifies the
 * [[com.github.osxhacker.demo.api.inventory.NewPhysicalInventory]] API type for
 * fitness of purpose and serves as an exemplar of its use.
 */
final class NewPhysicalInventorySpec ()
	extends ProjectSpec
		with Diagrams
		with ScalaCheckPropertyChecks
		with InventorySupport
{
	"The NewPhysicalInventory API type" must {
		"be able to contain values which satisfy its contract" in {
			forAll {
				inventory : NewPhysicalInventory =>
					assert (inventory.quantity.value > 0)
					assert (inventory.sku.value.nonEmpty)
				}
			}

		"be able to detect and reject invalid quantities" in {
			forAll (Gen.negNum[Int]) {
				quantity =>
					val invalid = NewPhysicalInventory.QuantityType.from (
						quantity
						)

					assert (
						invalid.isLeft,
						s"did not detect invalid quantity: $quantity"
						)
				}
			}

		"be able to detect and reject invalid sku's" in {
			/// Valid pattern: "^(?:[A-Z0-9]+-)*[A-Z0-9]$"
			val invalidSkus = Table (
				/// A sku must be uppercase.
				"abcde",

				/// Empty strings and those with spaces are not allowed.
				"", " ", " \t", "\t ", " \t ",

				/// Hyphens are allowed, but must be infix.
				"-ABC", "-", " -", "- ", " - ", "A-", "AB-",
				"A-BC-", "-A", "-AB", "-A-BC", "-A-", "-AB-",
				"-A-BC-"
				)

			forAll (invalidSkus) {
				candidate =>
					val result = NewPhysicalInventory.SkuType.from (candidate)

					assert (
						result.isLeft,
						s"failed to detect invalid sku: '$candidate'"
						)
				}
			}

		"be able to detect and reject invalid short descriptions" in {
			/// Valid pattern: "^[^ \t].+[^ \t]$"
			val invalidDescriptions = Table (
				/// Leading and trailing spaces are not allowed
				" test", "\ttest", " \ttest", "\t test",
				"TEST ", "TEST\t", "TEST \t", "TEST\t ",

				/// Must be at least 4 characters long
				"abc", "A B",

				/// Cannot exceed 64 characters
				"." * 65
				)

			forAll (invalidDescriptions) {
				candidate =>
					val result = NewPhysicalInventory.ShortDescriptionType.from (
						candidate
						)

					assert (
						result.isLeft,
						s"failed to detect invalid description: '$candidate'"
						)
				}
			}
		}
}

