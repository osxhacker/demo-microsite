package com.github.osxhacker.demo.storageFacility.adapter.database

import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec

import schema.StorageFacilityStatusRecord


/**
 * The '''CreateAndSeedTableForSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.storageFacility.adapter.database.CreateAndSeedTableFor]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class CreateAndSeedTableForSpec ()
	extends AnyWordSpec
		with Diagrams
{
	"CreateAndSeedTableFor" must {
		"be able to resolve known-supported record types" in {
			val result = CreateAndSeedTableFor[StorageFacilityStatusRecord] ()

			assert (result.isRight)
			assert (result.exists (_.nonEmpty))
			assert (
				result.exists (_.exists (_.sql.contains ("storage_facility_status")))
				)
			}

		"gracefully handle an unknown type being given" in {
			val result = CreateAndSeedTableFor[(Int, String)] ()

			assert (result.isLeft)
			}
		}
}

