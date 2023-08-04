package com.github.osxhacker.demo.storageFacility.domain.scenario

import cats.effect.IO
import eu.timepit.refined
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.FixtureAsyncWordSpecLike

import com.github.osxhacker.demo.chassis.domain.Slug
import com.github.osxhacker.demo.chassis.domain.entity.Identifier
import com.github.osxhacker.demo.chassis.domain.error.InvalidModelStateError
import com.github.osxhacker.demo.chassis.domain.event.{
	Region,
	SuppressEvents
	}

import com.github.osxhacker.demo.chassis.domain.repository.CreateIntent
import com.github.osxhacker.demo.storageFacility.domain.{
	Company,
	ScopedEnvironment,
	StorageFacility
	}

import com.github.osxhacker.demo.storageFacility.domain.event.AllStorageFacilityEvents


/**
 * The '''DeleteFacilitySpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.storageFacility.domain.scenario.DeleteFacility]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class DeleteFacilitySpec ()
	extends ScenarioSpec ()
		with FixtureAsyncWordSpecLike
		with Diagrams
		with SuppressEvents[ScopedEnvironment[IO], AllStorageFacilityEvents]
{
	/// Class Imports
	import cats.syntax.option._
	import refined.auto._


	/// Instance Properties
	private val save = SaveFacility[IO] ()
	private val find = FindFacility[IO] ()
	private val scenario = DeleteFacility[IO] ()


	"The DeleteFacility scenario" must {
		"be able to delete an existing facility" in {
			implicit env =>
				val result = for {
					saved <- save (CreateIntent (createFacility ()))
						.map (_.orFail ("save did not produce a result"))

					instance <- find (saved.id)
					answer <- scenario (instance)
				} yield answer

				result map {
					yesOrNo =>
						assert (yesOrNo === true)
					}
			}

		"gracefully handle when the facility does not exist" in {
			implicit env =>
				scenario (createFacility ()) map {
					yesOrNo =>
						assert (yesOrNo === false)
					}
			}

		"support multiple region invocations when enabled" in {
			implicit env =>
				val differentRegion = Region ("somewhere-else")
				val facility = StorageFacility.primary
					.replace (differentRegion.some) (createFacility ())

				assert (env.region !== differentRegion)

				for {
					saved <- save (CreateIntent (facility))
						.map (_.orFail ("save did not produce a result"))

					instance <- find (saved.id)
					answer <- scenario.enableMultiRegion () (instance)
					} yield assert (answer === true)
			}

		"fail when invoked with a different region and not enabled" in {
			implicit env =>
				val differentRegion = Region ("somewhere-else")
				val facility = StorageFacility.primary
					.replace (differentRegion.some) (createFacility ())

				assert (env.region !== differentRegion)

				val result = for {
					saved <- save (CreateIntent (facility))
						.map (_.orFail ("save did not produce a result"))

					instance <- find (saved.id)
					_ <- scenario (instance)
					} yield fail ("expected invocation to be rejected")

				result recover {
					case InvalidModelStateError (id, version, _, _) =>
						assert (id ne null)
						assert (version ne null)
					}
				}

		"fail when the facility has a different owner" in {
			implicit env =>
				val otherOwner = Company.slug
					.replace (Slug ("internal-devops"))
					.andThen (
						Company.id
							.replace (Identifier.fromRandom[Company] ())
						) (createArbitrary[Company] ())

				val facility = StorageFacility.owner
					.replace (otherOwner) (createFacility ())

				scenario (facility).attempt
					.map (_.fold (_ => succeed, _ => fail ()))
			}
		}
}
