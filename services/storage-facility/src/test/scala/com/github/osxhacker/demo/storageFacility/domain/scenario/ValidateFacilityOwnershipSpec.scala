package com.github.osxhacker.demo.storageFacility.domain.scenario

import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.storageFacility.domain.{
	Company,
	StorageFacility,
	StorageFacilitySupport
	}

import com.github.osxhacker.demo.storageFacility.domain.specification.FacilityBelongsTo


/**
 * The '''ValidateFacilityOwnershipSpec''' type defines the unit-tests which
 * certify
 * [[com.github.osxhacker.demo.storageFacility.domain.scenario.ValidateFacilityOwnership]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class ValidateFacilityOwnershipSpec ()
	extends AnyWordSpec
		with Diagrams
		with ProjectSpec
		with StorageFacilitySupport
{
	"The ValidateFacilityOwnership algorithm" must {
		"ensure a facility is belongs to its owner" in {
			val facility = createArbitrary[StorageFacility] ()

			assert (facility.belongsTo (facility.owner))
			assert (FacilityBelongsTo (facility.owner) (facility))
			}

		"ensure a facility does not belong to a different company" in {
			val facility = createArbitrary[StorageFacility] ()
			val other = createArbitrary[Company] ()

			assert (facility.owner.toRef () !== other.toRef ())
			assert (!facility.belongsTo (other))
			assert (!FacilityBelongsTo (other) (facility))
			}
		}
}

