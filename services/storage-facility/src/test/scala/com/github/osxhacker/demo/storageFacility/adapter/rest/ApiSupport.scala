package com.github.osxhacker.demo.storageFacility.adapter.rest

import java.time.OffsetDateTime

import org.scalacheck.{
	Arbitrary,
	Gen
	}

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.storageFacility.domain


/**
 * The '''ApiSupport''' type defines common behaviour for generating
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.api]] types.
 * By providing `implicit` [[org.scalacheck.Arbitrary]] methods for API types,
 * most having very specific [[eu.timepit.refined.api.Refined]] types,
 * generating instances has a __much__ higher probability of success.  Without
 * them, it would be virtually impossible to create a valid URN for example.
 */
trait ApiSupport
{
	/// Self Type Constraints
	this : ProjectSpec =>


	/// Instance Properties
	implicit protected lazy val apiStorageFacilityId =
		Generators.urn[api.StorageFacility.IdType] (
			domain.StorageFacility
				.storageFacilityNamespace ()
				.value
			)

	implicit protected lazy val apiStorageFacilityStatus =
		Gen.const (api.StorageFacilityStatus.Active)

	implicit protected lazy val apiStorageFacilityVersion =
		Gen.posNum[Int]
			.suchThat (_ < Int.MaxValue)
			.flatMap {
				value =>
					api.StorageFacility
						.VersionType
						.from (value)
						.fold (_ => Gen.fail, Gen.const)
				}

	/// Implicit Conversions
	implicit protected def arbApiNewStorageFacility (
		implicit status : Gen[api.StorageFacilityStatus]
		)
		: Arbitrary[api.NewStorageFacility] = {
		val generator = for {
			name <- Generators.trimmedString[api.NewStorageFacility.NameType] (
				2 to 64
				)

			theStatus <- status
			city <- Gen.oneOf (
				"New York" ::
				"Atlanta" ::
				"Chicago" ::
				"San Francisco" ::
				Nil
				)
				.toRefined[api.NewStorageFacility.CityType] ()

			state <- Generators.boundedString[api.NewStorageFacility.StateType] (
				2 to 3,
				Gen.alphaUpperChar
				)

			zip <- Gen.stringOfN (5, Gen.numChar)
				.toRefined[api.NewStorageFacility.ZipType] ()

			height <- Gen.choose (1.0, 30.0)
			length <- Gen.choose (10.0, 100.0)
			width <- Gen.choose (10.0, 100.0)
			volume <- Gen.const (height * length * width)
				.map (BigDecimal (_))
				.toRefined[api.NewStorageFacility.CapacityType] ()
		} yield api.NewStorageFacility (
			name = name,
			status = theStatus,
			city = city,
			state = state,
			zip = zip,
			capacity = volume,
			available = volume
			)

		Arbitrary (generator)
	}


	implicit protected def arbApiStorageFacility (
		implicit
		status : Gen[api.StorageFacilityStatus],
		storageFacilityId : Gen[api.StorageFacility.IdType],
		version : Gen[api.StorageFacility.VersionType]
		)
		: Arbitrary[api.StorageFacility] =
	{
		val generator = for {
			anId <- storageFacilityId
			theVersion <- version
			name <- Generators.trimmedString[api.StorageFacility.NameType] (
				2 to 64
				)

			theStatus <- status
			city <- Gen.oneOf (
				"New York" ::
				"Atlanta" ::
				"Chicago" ::
				"San Francisco" ::
				Nil
				)
				.toRefined[api.StorageFacility.CityType] ()

			state <- Generators.boundedString[api.StorageFacility.StateType] (
				2 to 3,
				Gen.alphaUpperChar
				)

			zip <- Gen.stringOfN (5, Gen.numChar)
				.toRefined[api.StorageFacility.ZipType] ()

			height <- Gen.choose (1.0, 30.0)
			length <- Gen.choose (10.0, 100.0)
			width <- Gen.choose (10.0, 100.0)
			volume <- Gen.const (height * length * width)
				.map (BigDecimal (_))
				.toRefined[api.StorageFacility.CapacityType] ()
		} yield api.StorageFacility (
			id = anId,
			version = theVersion,
			createdOn = OffsetDateTime.now (),
			lastChanged = OffsetDateTime.now (),
			name = name,
			status = theStatus,
			city = city,
			state = state,
			zip = zip,
			capacity = volume,
			available = volume
			)

		Arbitrary (generator)
	}
}

