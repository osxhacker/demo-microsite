package com.github.osxhacker.demo.storageFacility.domain.specification

import eu.timepit.refined
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.chassis.domain.event.Region
import com.github.osxhacker.demo.storageFacility.domain._


/**
 * The '''FacilityIsReadOnlySpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.storageFacility.domain.specification.FacilityIsActive]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class FacilityIsReadOnlySpec ()
	extends AnyWordSpec
		with Diagrams
		with ScalaCheckPropertyChecks
		with ProjectSpec
		with StorageFacilitySupport
{
	/// Class Imports
	import StorageFacility.{
		owner,
		primary
		}

	import cats.syntax.option._
	import refined.auto._


	/// Instance Properties
	private val companyIsActive = CompanyIsActive (owner)
	private val isReadOnly = FacilityIsReadOnly ()


	"The FacilityIsReadOnly isReadOnly" must {
		"be true when company is not active, primary is same" in {
			val facility = owner.andThen (Company.status)
				.replace (CompanyStatus.Inactive) (
					createArbitrary[StorageFacility] ()
					)

			val region = primary.get (facility)
				.orFail ("expected facility to have a primary region")

			assert (companyIsActive (facility) === false)
			assert (isReadOnly (facility -> region) === true)
		}

		"be true when company is not active, primary is empty" in {
			val facility = primary.replace (none[Region])
				.andThen (
					owner.andThen (Company.status)
						.replace (CompanyStatus.Inactive)
					) (createArbitrary[StorageFacility] ())

			assert (companyIsActive (facility) === false)
			assert (isReadOnly (facility -> defaultRegion) === true)
			}

		"be true when company is active, primary is different" in {
			val facility = createArbitrary[StorageFacility] ()
			val region = Region ("a-different-region")

			assert (primary.get (facility).isDefined)
			assert (primary.get (facility).exists (_ === region) === false)
			assert (companyIsActive (facility) === true)
			assert (isReadOnly (facility -> region) === true)
		}

		"be false when company is active, primary is same" in {
			val facility = createArbitrary[StorageFacility] ()
			val region = primary.get (facility)
				.orFail ("expected facility to have a primary region")

			assert (companyIsActive (facility) === true)
			assert (isReadOnly (facility -> region) === false)
			}

		"be false when company is active, primary is empty" in {
			val facility = primary.replace (none[Region]) (
				createArbitrary[StorageFacility] ()
				)

			assert (companyIsActive (facility) === true)
			assert (isReadOnly (facility -> defaultRegion) === false)
			}
		}
}

