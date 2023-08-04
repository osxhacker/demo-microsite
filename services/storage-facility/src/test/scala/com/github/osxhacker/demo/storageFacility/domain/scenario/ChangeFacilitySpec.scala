package com.github.osxhacker.demo.storageFacility.domain.scenario

import scala.language.postfixOps

import cats.data.Kleisli
import cats.effect.IO
import eu.timepit.refined
import monocle.Getter
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.FixtureAsyncWordSpecLike
import shapeless.{
	syntax => _,
	_
	}

import squants.space.{
	CubicMeters,
	Volume
	}

import com.github.osxhacker.demo.chassis.domain.{
	ErrorOr,
	Slug
	}

import com.github.osxhacker.demo.chassis.domain.entity._
import com.github.osxhacker.demo.chassis.domain.error.{
	InvalidModelStateError,
	StaleObjectError,
	ValidationError
	}

import com.github.osxhacker.demo.chassis.domain.event.{
	Region,
	SuppressEvents
	}

import com.github.osxhacker.demo.chassis.domain.repository.CreateIntent
import com.github.osxhacker.demo.storageFacility.domain._
import com.github.osxhacker.demo.storageFacility.domain.event.AllStorageFacilityEvents


/**
 * The '''ChangeFacilitySpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.storageFacility.domain.scenario.ChangeFacility]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class ChangeFacilitySpec ()
	extends ScenarioSpec ()
		with FixtureAsyncWordSpecLike
		with Diagrams
		with SuppressEvents[ScopedEnvironment[IO], AllStorageFacilityEvents]
{
	/// Class Imports
	import StorageFacility._
	import cats.syntax.applicative._
	import cats.syntax.either._
	import cats.syntax.option._
	import refined.api.Refined
	import refined.api.RefType.applyRef
	import refined.auto._
	import refined.numeric.NonNegative


	/// Instance Properties
	private val factory =
		Kleisli[
			ErrorOr,
			StorageFacility :: Region :: Company :: HNil,
			StorageFacility
			] {
			case facility :: region :: _ :: HNil =>
				StorageFacility.primary
					.replace (region.some) (facility)
					.pure[ErrorOr]
			}

	private val volumeToDouble =
		Getter[Volume, Refined[Double, NonNegative]] {
			volume =>
				applyRef[Refined[Double, NonNegative]] (volume.value)
					.valueOr (fail (_))
		}

	private val scenario = ChangeFacility[IO] (
		id = StorageFacility.id
			.asGetter,

		version = StorageFacility.version
			.asGetter
			.andThen (Version.value),

		status = StorageFacility.status
			.asGetter,

		available = StorageFacility.available
			.asGetter
			.andThen (volumeToDouble),

		capacity = StorageFacility.capacity
			.asGetter
			.andThen (volumeToDouble)
		) (factory)

	private val save = SaveFacility[IO] ()


	"The ChangeFacility scenario" must {
		"accept a valid API StorageFacility change" in {
			implicit env =>
				val unsaved = createFacility ("Initial facility")
				val result = for {
					initial <- save (CreateIntent (unsaved))
						.map (_.orFail ("expected save to produce facility"))

					altered <- initial.changeStatusTo[IO] (
						StorageFacilityStatus.Closed
						)

					saved <- scenario (initial, altered)
					} yield saved

				result map {
					instance =>
						assert (instance.id === unsaved.id)
						assert (instance.version > Version.initial)
						assert (instance.version > unsaved.version)
					}
			}

		"ignore a change without differences" in {
			implicit env =>
				val unsaved = createFacility ("Initial facility")
				val result = for {
					initial <- save (CreateIntent (unsaved))
						.map (_.orFail ("expected save to produce facility"))

					latest <- scenario (initial, initial)
					} yield (initial, latest)

				result map {
					case (first, second) =>
						assert (first === second)
					}
			}

		"allow changes when in original region is empty" in {
			implicit env =>
				val facility = primary.replace (none[Region]) (
					createFacility ("The warehouse", predefined.tenant)
					)

				val result = for {
					initial <- save (CreateIntent (facility))
						.map (_.orFail ("expected save to produce facility"))

					altered <- initial.changeStatusTo[IO] (
						StorageFacilityStatus.Closed
						)

					saved <- scenario (initial, altered)
				} yield saved

				result map {
					latest =>
						assert (id.get (facility) === id.get (latest))
						assert (version.get (facility) < version.get (latest))
						assert (primary.get (facility).isEmpty)
						assert (primary.get (latest).isDefined)
					}
			}

		"allow changes when in a different region and enabled" in {
			implicit env =>
				val differentRegion = Region ("somewhere-else")
				val facility = primary.replace (differentRegion.some) (
					createFacility ("The warehouse", predefined.tenant)
					)

				assert (env.region !== differentRegion)

				val result = for {
					initial <- save (CreateIntent (facility))
						.map (_.orFail ("expected save to produce facility"))

					altered <- initial.changeStatusTo[IO] (
						StorageFacilityStatus.Closed
						)

					saved <- scenario.enableMultiRegion () (initial, altered)
					} yield saved

				result map {
					latest =>
						assert (id.get (facility) === id.get (latest))
						assert (version.get (facility) < version.get (latest))
					}
			}

		"detect stale objects" in {
			implicit env =>
				val unsaved = createFacility ("Original facility")
				val result = for {
					initial <- save (CreateIntent (unsaved))
						.map (_.orFail ("expected save to produce facility"))

					altered <- initial.changeStatusTo[IO] (
						StorageFacilityStatus.Closed
						)

					first <- scenario (initial, altered)
					anotherChange <- first.changeStatusTo[IO] (
						StorageFacilityStatus.Active
						)

					_ <- scenario (anotherChange, initial)
					} yield fail ("expected second invocation to fail")

				result recover {
					case _ : StaleObjectError[_] =>
						succeed
					}
			}

		"reject changes with a 'future version'" in {
			implicit env =>
				val unsaved = createFacility ("Initial facility")
				val result = for {
					initial <- save (CreateIntent (unsaved))
						.map (_.orFail ("expected save to produce facility"))

					altered <- initial.touch[IO] ()

					_ <- scenario (initial, altered)
					} yield fail ("expected invocation to be rejected")

				result recover {
					case ValidationError (errors) =>
						assert (errors.length === 1L)
					}
			}

		"reject a change having available more than capacity" in {
			implicit env =>
				val unsaved = createFacility ("Initial facility")
				val result = for {
					initial <- save (CreateIntent (unsaved))
						.map (_.orFail ("expected save to produce facility"))

					altered = available.replace (
						initial.capacity + CubicMeters (0.1)
						) (initial)

					_ <- scenario (initial, altered)
					} yield fail ("expected invocation to be rejected")

				result recover {
					case ValidationError (errors) =>
						assert (errors.length === 1L)
					}
			}

		"reject a change when in a different region and not enabled" in {
			implicit env =>
				val differentRegion = Region ("somewhere-else")
				val facility = primary.replace (differentRegion.some) (
					createFacility ("The warehouse", predefined.tenant)
					)

				assert (env.region !== differentRegion)

				val result = for {
					initial <- save (CreateIntent (facility))
						.map (_.orFail ("expected save to produce facility"))

					altered <- initial.changeStatusTo[IO] (
						StorageFacilityStatus.Closed
						)

					_ <- scenario (initial, altered)
					} yield fail ("expected invocation to produce an error")

				result recover {
					case InvalidModelStateError (theId, theVersion, _, _) =>
						assert (id.get (facility) === theId)
						assert (version.get (facility) <= theVersion)
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

				val facility = owner.replace (otherOwner) (createFacility ())

				scenario (facility, facility).attempt
					.map (_.fold (_ => succeed, _ => fail ()))
		}
	}
}

