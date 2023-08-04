package com.github.osxhacker.demo.storageFacility.domain.scenario

import cats.effect.IO
import eu.timepit.refined
import org.scalatest.diagrams.Diagrams

import com.github.osxhacker.demo.chassis.domain.event.Region
import com.github.osxhacker.demo.storageFacility.domain.StorageFacility
import com.github.osxhacker.demo.storageFacility.domain.event._


/**
 * The '''InterpretStorageFacilityEventsSpec''' type defines the unit-tests which
 * certify
 * [[com.github.osxhacker.demo.storageFacility.domain.scenario.InterpretStorageFacilityEvents]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class InterpretStorageFacilityEventsSpec ()
	extends ScenarioSpec ()
		with Diagrams
{
	/// Class Imports
	import refined.auto._
	import shapeless.syntax.inject._


	/// Instance Properties
	val facility = createFacility ("A new facility", predefined.tenant)


	"The InterpretStorageFacilityEvents use-case scenario" must {
		"only require domain types" in {
			_ =>
				InterpretStorageFacilityEvents[IO] ()

				succeed
			}

		"fail gracefully if the company is unknown" in {
			implicit env =>
				val global = createGlobalEnvironment (env)
				val interpreter = InterpretStorageFacilityEvents[IO] ()
				val unknownCompany = createFacility ()

				for {
					_ <- interpreter () (
						StorageFacilityCreated (unknownCompany)
							.inject[AllStorageFacilityEvents] -> global
						)

					result <- env.storageFacilities
						.find (unknownCompany.id)
						.attempt
					} yield assert (result.isLeft)
			}

		"be able to interpret created events" in {
			implicit env =>
				val global = createGlobalEnvironment (env)
				val interpreter = InterpretStorageFacilityEvents[IO] ()

				for {
					_ <- interpreter () (
						StorageFacilityCreated (facility)
							.inject[AllStorageFacilityEvents] -> global
						)

					result <- env.storageFacilities
						.find (facility.id)
					} yield assert (result.id === facility.id)
			}

		"be able to interpret deleted events (same region)" in {
			implicit env =>
				val global = createGlobalEnvironment (env)
				val interpreter = InterpretStorageFacilityEvents[IO] ()

				for {
					_ <- interpreter () (
						StorageFacilityCreated (facility)
							.inject[AllStorageFacilityEvents] -> global
						)

					_ <- env.storageFacilities
						.find (facility.id)

					_ <- interpreter () (
						StorageFacilityDeleted (facility)
							.inject[AllStorageFacilityEvents] -> global
						)

					afterDelete <- env.storageFacilities
						.find (facility.id)
						.attempt
					} yield assert (afterDelete.isLeft)
			}

		"be able to interpret deleted events (from different region)" in {
			implicit env =>
				val global = createGlobalEnvironment (env)
				val interpreter = InterpretStorageFacilityEvents[IO] ()

				assert (!Region.value.get (global.region).startsWith ("a-diff"))
				assert (facility.primary.isDefined)

				for {
					_ <- interpreter () (
						StorageFacilityCreated (facility)
							.inject[AllStorageFacilityEvents] -> global
						)

					_ <- env.storageFacilities
						.find (facility.id)

					_ <- interpreter () (
						StorageFacilityDeleted (facility)
							.inject[AllStorageFacilityEvents] -> global
						)

					afterDelete <- env.storageFacilities
						.find (facility.id)
						.attempt
					} yield assert (afterDelete.isLeft)
			}

		"be able to interpret profile changed events" in {
			implicit env =>
				import StorageFacility.name


				val global = createGlobalEnvironment (env)
				val interpreter = InterpretStorageFacilityEvents[IO] ()
				val altered = StorageFacility.name
					.replace ("Another name") (facility)

				assert (name.get (facility) !== name.get (altered))

				for {
					_ <- interpreter () (
						StorageFacilityCreated (facility)
							.inject[AllStorageFacilityEvents] -> global
						)

					_ <- interpreter () (
						StorageFacilityProfileChanged (altered)
							.inject[AllStorageFacilityEvents] -> global
						)

					result <- env.storageFacilities
						.find (facility.id)
				} yield {
					assert (result.id === facility.id)
					assert (result.name === altered.name)
					}
			}
		}
}

