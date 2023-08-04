package com.github.osxhacker.demo.storageFacility.domain.scenario

import cats.effect.IO
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.FixtureAsyncWordSpecLike

import com.github.osxhacker.demo.chassis.domain.repository._


/**
 * The '''SaveFacilitySpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.storageFacility.domain.scenario.SaveFacility]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class SaveFacilitySpec ()
	extends ScenarioSpec ()
		with FixtureAsyncWordSpecLike
		with Diagrams
{
	/// Instance Properties
	private val scenario = SaveFacility[IO] ()


	"The SaveFacility scenario" must {
		"be able to save a new facility" in {
			implicit env =>
				val instance = createFacility ()
				val result = scenario (CreateIntent (instance))

				result map {
					facility =>
						assert (facility.isDefined)
						assert (facility.exists (_.id === instance.id))
						assert (facility.exists (_.version >= instance.version))
					}
			}
		}
}
