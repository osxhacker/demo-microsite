package com.github.osxhacker.demo.storageFacility.domain.scenario

import cats.effect.IO
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.FixtureAsyncWordSpecLike

import com.github.osxhacker.demo.chassis.domain.entity._
import com.github.osxhacker.demo.chassis.domain.error.ObjectNotFoundError
import com.github.osxhacker.demo.chassis.domain.repository._
import com.github.osxhacker.demo.storageFacility.domain._


/**
 * The '''FindFacilitySpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.storageFacility.domain.scenario.FindFacility]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class FindFacilitySpec ()
	extends ScenarioSpec ()
		with FixtureAsyncWordSpecLike
		with Diagrams
{
	/// Instance Properties
	private val save = SaveFacility[IO] ()
	private val scenario = FindFacility[IO] ()


	"The FindFacility scenario" must {
		"be able to find an existing facility" in {
			implicit env =>
				val result = for {
					saved <- save (CreateIntent (createFacility ()))
						.map (_.orFail ("save did not produce a result"))

					answer <- scenario (saved.id)
					} yield answer

				result map {
					instance =>
						assert (instance.version >= Version.initial)
				}
			}

		"fail when the facility does not exist" in {
			implicit env =>
				scenario (createFacility ().id).map (_ => fail ())
					.recover {
						case _ : ObjectNotFoundError[_] =>
							succeed
						}
			}
		}
}

