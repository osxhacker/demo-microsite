package com.github.osxhacker.demo.storageFacility.domain

import scala.language.{
	implicitConversions,
	postfixOps
	}

import eu.timepit.refined
import org.scalacheck.{
	Arbitrary,
	Gen
	}

import squants.space.CubicMeters

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.chassis.domain.entity._
import com.github.osxhacker.demo.chassis.domain.event.Region


/**
 * The '''StorageFacilitySupport''' type defines common behaviour for
 * generating
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]] types.
 * By providing `implicit` [[org.scalacheck.Arbitrary]] methods for storage
 * facility types having very specific [[eu.timepit.refined.api.Refined]] types,
 * generating instances has a __much__ higher probability of success.  Without
 * them, it would be virtually impossible to create a valid URN for example.
 */
trait StorageFacilitySupport
	extends CompanySupport
{
	/// Self Type Constraints
	this : ProjectSpec =>


	/// Class Imports
	import cats.syntax.option._
	import refined.auto._


	/// Instance Properties
	protected val defaultRegion = Region ("unit-testing")


	/// Implicit Conversions
	implicit protected val domainRegion = Gen.const[Option[Region]] (
		defaultRegion.some
		)

	implicit protected val domainStorageFacilityStatus =
		Gen.const[StorageFacilityStatus] (StorageFacilityStatus.Active)

	implicit protected val domainVersion =
		Gen.posNum[Int]
			.suchThat (_ < Int.MaxValue)
			.flatMap {
				value =>
					Version[ErrorOr] (value).fold (_ => Gen.fail, Gen.const)
				}


	implicit protected def arbDomainStorageFacility (
		implicit
		company : Arbitrary[Company],
		region : Gen[Option[Region]],
		status : Gen[StorageFacilityStatus],
		version : Gen[Version]
		)
		: Arbitrary[StorageFacility] =
	{
		val generator = for {
			anId <- Gen.const (Identifier.fromRandom[StorageFacility] ())
			theVersion <- version
			owner <- company.arbitrary
			region <- region
			name <- Generators.trimmedString[StorageFacility.Name] (
				2 to 64
				)

			status <- status
			city <- Gen.oneOf (
				"New York" ::
				"Atlanta" ::
				"Chicago" ::
				"San Francisco" ::
				Nil
				)
				.toRefined[StorageFacility.City] ()

			state <- Generators.boundedString[StorageFacility.State] (
				2 to 3,
				Gen.alphaUpperChar
				)

			zip <- Gen.stringOfN (5, Gen.numChar)
				.toRefined[StorageFacility.Zip] ()

			height <- Gen.choose (1.0, 30.0)
			length <- Gen.choose (10.0, 100.0)
			width <- Gen.choose (10.0, 100.0)
			volume <- Gen.const (height * length * width)
				.map (BigDecimal (_))
		} yield StorageFacility (
			id = anId,
			version = theVersion,
			owner = owner,
			primary = region,
			status = status,
			name = name,
			city = city,
			state = state,
			zip = zip,
			capacity = CubicMeters (volume),
			available = CubicMeters (volume),
			timestamps = ModificationTimes.now ()
			)

		Arbitrary (generator)
	}
}
