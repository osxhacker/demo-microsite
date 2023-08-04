package com.github.osxhacker.demo.api.inventory

import java.time.OffsetDateTime

import org.scalacheck.{
	Arbitrary,
	Gen
	}

import com.github.osxhacker.demo.api.ProjectSpec


/**
 * The '''InventorySupport''' type defines common behaviour for interacting with
 * [[com.github.osxhacker.demo.api.inventory]] types.  By providing `implicit`
 * [[org.scalacheck.Arbitrary]] methods for inventory types having very
 * specific [[eu.timepit.refined.api.Refined]] types, generating instances has
 * a __much__ higher probability of success.  Without them, it would be
 * virtually impossible to create a valid URN for example.
 */
trait InventorySupport
{
	/// Self Type Constraints
	this : ProjectSpec =>


	/// Instance Properties
	protected val physicalInventoryId =
		Generators.urn[PhysicalInventory.IdType] ("test:physical-inventory")

	protected val virtualInventoryId =
		Generators.urn[VirtualInventory.IdType] ("test:virtual-inventory")


	/// Implicit Conversions
	protected implicit def arbNewPhysicalInventory (
		implicit
		quantity : Arbitrary[NewPhysicalInventory.QuantityType],
		dimension : Arbitrary[Dimension]
		)
		: Arbitrary[NewPhysicalInventory] =
	{
		val generator = for {
			sku <- Generators.boundedString[NewPhysicalInventory.SkuType] (
				2 to 32,
				Gen.frequency (
					2 -> Gen.alphaUpperChar,
					1 -> Gen.numChar
					)
				)

			desc <- Generators.boundedString[NewPhysicalInventory.ShortDescriptionType](
				4 to 64
				)

			howMany <- quantity.arbitrary
			volume <- dimension.arbitrary
			storage <- Generators.urn[NewPhysicalInventory.FacilityType] (
				"storage-facility"
				)
		} yield NewPhysicalInventory (
			sku = sku,
			shortDescription = desc,
			quantity = howMany,
			size = volume,
			facility = storage
			)

		Arbitrary (generator)
	}


	protected implicit def arbPhysicalInventory (
		implicit
		version : Arbitrary[PhysicalInventory.VersionType],
		quantity : Arbitrary[PhysicalInventory.QuantityType],
		dimension : Arbitrary[Dimension]
		)
		: Arbitrary[PhysicalInventory] =
	{
		val generator = for {
			id <- physicalInventoryId
			theVersion <- version.arbitrary
			sku <- Generators.boundedString[PhysicalInventory.SkuType] (
				2 to 32,
				Gen.frequency (
					2 -> Gen.alphaUpperChar,
					1 -> Gen.numChar
					)
				)

			desc <- Generators.boundedString[PhysicalInventory.ShortDescriptionType](
				4 to 64
				)

			howMany <- quantity.arbitrary
			volume <- dimension.arbitrary
			storage <- Generators.urn[PhysicalInventory.FacilityType] (
				"storage-facility"
				)
		} yield PhysicalInventory (
			id = id,
			version = theVersion,
			createdOn = OffsetDateTime.now (),
			lastChanged = OffsetDateTime.now (),
			sku = sku,
			shortDescription = desc,
			quantity = howMany,
			size = volume,
			facility = storage
			)

		Arbitrary (generator)
	}


	protected implicit def arbNewVirtualInventory
		: Arbitrary[NewVirtualInventory] =
	{
		val generator = for {
			sku <- Generators.boundedString[NewVirtualInventory.SkuType] (
				4 to 16,
				Gen.frequency (
					1 -> Gen.alphaUpperChar,
					1 -> Gen.numChar
					)
				)

			desc <- Generators.boundedString[NewVirtualInventory.ShortDescriptionType](
				4 to 64
				)
		} yield NewVirtualInventory (
			sku = sku,
			shortDescription = desc
			)

		Arbitrary (generator)
	}


	protected implicit def arbVirtualInventory (
		implicit version : Arbitrary[VirtualInventory.VersionType]
		)
		: Arbitrary[VirtualInventory] =
	{
		val generator = for {
			id <- virtualInventoryId
			theVersion <- version.arbitrary
			sku <- Generators.boundedString[VirtualInventory.SkuType] (
				4 to 16,
				Gen.frequency (
					1 -> Gen.alphaUpperChar,
					1 -> Gen.numChar
					)
				)

			desc <- Generators.boundedString[VirtualInventory.ShortDescriptionType](
				4 to 64
				)
			} yield VirtualInventory (
				id = id,
				version = theVersion,
				createdOn = OffsetDateTime.now (),
				lastChanged = OffsetDateTime.now (),
				sku = sku,
				shortDescription = desc
				)

		Arbitrary (generator)
	}
}
