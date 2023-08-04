package com.github.osxhacker.demo.storageFacility.domain.scenario

import cats.data.Kleisli
import cats.effect.IO
import eu.timepit.refined
import monocle.macros.GenLens
import kamon.testkit.InitAndStopKamonAfterAll
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.FixtureAsyncWordSpecLike
import shapeless.{
	syntax => _,
	_
	}

import squants.space.CubicMeters

import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.chassis.domain.entity._
import com.github.osxhacker.demo.chassis.domain.error.{
	ConflictingObjectsError,
	ValidationError
	}

import com.github.osxhacker.demo.chassis.domain.event.{
	Region,
	SuppressEvents
	}

import com.github.osxhacker.demo.storageFacility.adapter.rest.api
import com.github.osxhacker.demo.storageFacility.domain.{
	Company,
	ScopedEnvironment,
	StorageFacility,
	StorageFacilityStatus
	}

import com.github.osxhacker.demo.storageFacility.domain.event.AllStorageFacilityEvents


/**
 * The '''CreateFacilitySpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.storageFacility.domain.scenario.CreateFacility]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class CreateFacilitySpec ()
	extends ScenarioSpec ()
		with FixtureAsyncWordSpecLike
		with Diagrams
		with InitAndStopKamonAfterAll
		with SuppressEvents[ScopedEnvironment[IO], AllStorageFacilityEvents]
{
	/// Class Imports
	import api.NewStorageFacility.Optics
	import cats.syntax.all._
	import refined.api.RefType


	/// Class Types
	final type FactoryParamsType =
		api.NewStorageFacility ::
		Region ::
		Company ::
		HNil


	/// Instance Properties
	private val factory = Kleisli[ErrorOr, FactoryParamsType, StorageFacility] {
		case nsf :: region :: owner :: HNil =>
			StorageFacility (
				id = Identifier.fromRandom[StorageFacility] (),
				version = Version.initial,
				owner = owner,
				status = StorageFacilityStatus.withNameInsensitive (
					nsf.status.toString
					),

				name = RefType.applyRef[StorageFacility.Name] (nsf.name.value)
					.leftMap (new IllegalArgumentException (_))
					.orFail (),

				city = RefType.applyRef[StorageFacility.City] (nsf.city.value)
					.leftMap (new IllegalArgumentException (_))
					.orFail (),

				state = RefType.applyRef[StorageFacility.State] (nsf.state.value)
					.leftMap (new IllegalArgumentException (_))
					.orFail (),

				zip = RefType.applyRef[StorageFacility.Zip] (nsf.zip.value)
					.leftMap (new IllegalArgumentException (_))
					.orFail (),

				capacity = CubicMeters (nsf.capacity.value),
				available = CubicMeters (nsf.available.value),
				timestamps = ModificationTimes.now ()
				)
				.pure[ErrorOr]
		}

	private val owner = createArbitrary[Company] ()
	private val scenario =
		CreateFacility[IO] (
			available = GenLens[FactoryParamsType] (_.head) andThen Optics.available,
			capacity = GenLens[FactoryParamsType] (_.head) andThen Optics.capacity
			) (factory)


	"The CreateFacility scenario" must {
		"accept a valid NewStorageFacility instance" in {
			implicit env =>
				val result = createNewFacility () >>= {
					facility =>
						scenario (facility :: env.region :: owner :: HNil)
					}

				result map {
					instance =>
						assert (instance.version >= Version.initial)
					}
			}

		"accept identical NewStorageFacility instances" in {
			implicit env =>
				val result = createNewFacility ().flatMap {
					instance =>
						scenario (instance :: env.region :: owner :: HNil).product (
							scenario (instance :: env.region :: owner :: HNil)
							)
					}

				result map {
					case (first, second) =>
						assert (first.id eqv second.id)
						assert (first.version eqv second.version)
						assert (first.name === second.name)
					}
			}

		"reject when available exceeds capacity" in {
			implicit env =>
				val result = for {
					instance <- createNewFacility ()
					exceeds <- api.NewStorageFacility.AvailableType
						.from (instance.capacity.value + 10)
						.liftTo[IO]

					answer <- scenario (
						instance.copy (available = exceeds) ::
						env.region ::
						owner ::
						HNil
						)
					} yield answer

				result.attempt map {
					case Left (e : ValidationError[_]) =>
						assert (e.getMessage.contains ("StorageFacility"))

					case other =>
						fail ("unexpected result: " + other)
					}
			}

		"reject differing NewStorageFacility instances with same name" in {
			implicit env =>
				val result = for {
					instance <- createNewFacility ()
					reduced <- api.NewStorageFacility.AvailableType
						.from (instance.available.value - 50)
						.liftTo[IO]

					first <- scenario (instance :: env.region :: owner :: HNil)
					second <- scenario (
						instance.copy (available = reduced) ::
						env.region ::
						owner ::
						HNil
						)
					} yield first -> second

				result.attempt map {
					case Left (e @ ConflictingObjectsError (message, cause)) =>
						assert (message.nonEmpty)
						assert (cause.isEmpty)
						assert (e.getMessage.contains (message))

					case other =>
						fail ("unexpected result: " + other)
					}
		}
	}


	private def createNewFacility () : IO[api.NewStorageFacility] =
		api.NewStorageFacility.from (
			name = "new facility test",
			status = api.StorageFacilityStatus.Active,
			city = "Anytown",
			state = "KS",
			zip = "12345",
			capacity = 1_000,
			available = 1_000
			)
			.liftTo[IO]
}

