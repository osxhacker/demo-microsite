package com.github.osxhacker.demo.api.storageFacility

import java.time.OffsetDateTime

import org.scalacheck.{
	Arbitrary,
	Gen
	}

import com.github.osxhacker.demo.api.ProjectSpec


/**
 * The '''StorageFacilitySupport''' type defines common behaviour for
 * interacting with [[com.github.osxhacker.demo.api.storageFacility]] types.  By
 * providing `implicit` [[org.scalacheck.Arbitrary]] methods for storage
 * facility types having very specific [[eu.timepit.refined.api.Refined]] types,
 * generating instances has a __much__ higher probability of success.  Without
 * them, it would be virtually impossible to create a valid URN for example.
 */
trait StorageFacilitySupport
{
	/// Self Type Constraints
	this : ProjectSpec =>


	/// Instance Properties
	implicit protected val storageFacilityId =
		Generators.urn[StorageFacility.IdType] ("test:storage-facility")

	implicit protected lazy val storageFacilityVersion =
		Gen.posNum[Int]
			.suchThat (_ < Int.MaxValue)
			.flatMap {
				value =>
					StorageFacility.VersionType
						.from (value)
						.fold (_ => Gen.fail, Gen.const)
				}


	/// Implicit Conversions
	protected implicit val arbNewStorageFacility
		: Arbitrary[NewStorageFacility] =
	{
		val generator = for {
			name <- Generators.boundedString[NewStorageFacility.NameType] (
				2 to 64,
				Gen.asciiPrintableChar
					.filterNot (_.isWhitespace)
				)

			city <- Gen.oneOf (
				"New York" ::
				"Atlanta" ::
				"Chicago" ::
				"San Francisco" ::
				Nil
				)
				.toRefined[NewStorageFacility.CityType] ()

			state <- Generators.boundedString[NewStorageFacility.StateType] (
				2 to 3,
				Gen.alphaUpperChar
				)

			zip <- Gen.stringOfN (5, Gen.numChar)
				.toRefined[NewStorageFacility.ZipType] ()

			height <- Gen.choose (1.0, 33.0)
			length <- Gen.choose (10.0, 100.0)
			width <- Gen.choose (10.0, 100.0)
			volume <- Gen.const (height * length * width)
				.map (BigDecimal (_))
				.toRefined[StorageFacility.CapacityType] ()
		} yield NewStorageFacility (
			name = name,
			status = StorageFacilityStatus.Active,
			city = city,
			state = state,
			zip = zip,
			capacity = volume,
			available = volume
			)

		Arbitrary (generator)
	}


	protected implicit def arbStorageFacility (
		implicit
		id : Gen[StorageFacility.IdType],
		version : Gen[StorageFacility.VersionType]
		)
		: Arbitrary[StorageFacility] =
	{
		val generator = for {
			anId <- id
			theVersion <- version
			name <- Generators.boundedString[StorageFacility.NameType] (
				2 to 64,
				Gen.asciiPrintableChar
					.filterNot (_.isWhitespace)
				)

			city <- Gen.oneOf (
				"New York" ::
				"Atlanta" ::
				"Chicago" ::
				"San Francisco" ::
				Nil
				)
				.toRefined[StorageFacility.CityType] ()

			state <- Generators.boundedString[StorageFacility.StateType] (
				2 to 3,
				Gen.alphaUpperChar
				)

			zip <- Gen.stringOfN (5, Gen.numChar)
				.toRefined[StorageFacility.ZipType] ()

			height <- Gen.choose (1.0, 33.0)
			length <- Gen.choose (10.0, 100.0)
			width <- Gen.choose (10.0, 100.0)
			volume <- Gen.const (height * length * width)
				.map (BigDecimal (_))
				.toRefined[StorageFacility.CapacityType] ()
		} yield StorageFacility (
			id = anId,
			version = theVersion,
			createdOn = OffsetDateTime.now (),
			lastChanged = OffsetDateTime.now (),
			name = name,
			status = StorageFacilityStatus.Active,
			city = city,
			state = state,
			zip = zip,
			capacity = volume,
			available = volume
			)

		Arbitrary (generator)
	}
}

