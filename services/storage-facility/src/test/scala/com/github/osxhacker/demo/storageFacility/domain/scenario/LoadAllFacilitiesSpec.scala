package com.github.osxhacker.demo.storageFacility.domain.scenario

import cats.effect.IO
import eu.timepit.refined
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.FixtureAsyncWordSpecLike

import com.github.osxhacker.demo.chassis.domain.repository.{
	CreateIntent,
	Intent
	}

import com.github.osxhacker.demo.storageFacility.domain.StorageFacility


/**
 * The '''LoadAllFacilitiesSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.storageFacility.domain.scenario.LoadAllFacilities]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class LoadAllFacilitiesSpec ()
	extends ScenarioSpec ()
		with FixtureAsyncWordSpecLike
		with Diagrams
{
	/// Class Imports
	import cats.syntax.traverse._
	import refined.auto._


	/// Instance Properties
	private val save = SaveFacility[IO] ()
	private val scenario = LoadAllFacilities[IO] ()


	"The LoadAllFacilities scenario" must {
		"be able to retrieve all saved instances" in {
			implicit env =>
				val unsaved = List[Intent[StorageFacility]] (
					CreateIntent (createFacility ("First", predefined.tenant)),
					CreateIntent (createFacility ("Second", predefined.tenant)),
					CreateIntent (createFacility ("Third", predefined.tenant))
					)

				val result = for {
					saved <- unsaved.traverse (save (_))
					stream <- scenario (predefined.tenant)
					loaded <- stream.compile.toList
					} yield (saved, loaded)

				result map {
					case (expected, actual) =>
						assert (expected.size === actual.size)
						assert (expected.forall (_.isDefined))
					}
			}

		"gracefully handle when there are no instances" in {
			implicit env =>
				scenario (predefined.tenant).flatMap (_.compile.toList)
					.map {
						instances =>
							assert (instances.isEmpty)
						}
			}
		}
}

