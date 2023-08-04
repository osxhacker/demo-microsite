package com.github.osxhacker.demo.storageFacility.domain

import java.time.Instant
import java.util.UUID.randomUUID

import eu.timepit.refined
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec
import org.typelevel.log4cats.noop.NoOpFactory
import shapeless.CNil

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.chassis.domain.entity._
import com.github.osxhacker.demo.chassis.domain.event.{
	EventSupport,
	Region
	}

import com.github.osxhacker.demo.chassis.monitoring.{
	CorrelationId,
	Subsystem
	}

import com.github.osxhacker.demo.storageFacility.adapter.database.{
	AlwaysFailCompanyRepository,
	AlwaysFailStorageFacilityRepository
	}

import com.github.osxhacker.demo.storageFacility.domain.event.{
	AllStorageFacilityEvents,
	EventChannel,
	StorageFacilityChangeEvents
	}


/**
 * The '''StorageFacilitySpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]] for
 * fitness of purpose and serves as an exemplar of its use.
 */
final class StorageFacilitySpec ()
	extends AnyWordSpec
		with Diagrams
		with ProjectSpec
		with StorageFacilitySupport
		with EventSupport
		with ScalaCheckPropertyChecks
{
	/// Class Imports
	import refined.auto._


	/// Instance Properties
	implicit private val loggerFactory = NoOpFactory[ErrorOr]
	implicit private val subsystem = Subsystem ("unit-test")
	implicit private val env = ScopedEnvironment[ErrorOr] (
		GlobalEnvironment[ErrorOr] (
			defaultRegion,
			CompanyReference (Identifier.fromRandom[Company]()),
			AlwaysFailCompanyRepository[ErrorOr] (new RuntimeException ()),
			AlwaysFailStorageFacilityRepository[ErrorOr] (
				new RuntimeException ()
				),

			new MockEventProducer[ErrorOr, EventChannel, AllStorageFacilityEvents] (
				EventChannel.UnitTest
				)
			),

		CompanyReference (Identifier.fromRandom[Company] ()),
		new CorrelationId (randomUUID ()),
		)


	"The StorageFacility entity" must {
		"define equality by id and version alone" in {
			forAll {
				instance : StorageFacility =>
					val changed = StorageFacility.name
						.replace ("A different name") (instance)

					assert (instance === instance)
					assert (instance === changed)
				}
			}

		"be able to detect changes other than modification times" in {
			forAll {
				instance : StorageFacility =>
					val differentStatus = instance.changeStatusTo[ErrorOr] (
						StorageFacilityStatus.Closed
						)
						.orFail ()

					val changedTimestamps = instance.copy (
						timestamps = ModificationTimes (
							createdOn = Instant.ofEpochSecond (0L),
							lastChanged = Instant.ofEpochSecond (0L)
						)
					)

					assert (instance.differsFrom (instance) === false)
					assert (instance.differsFrom (differentStatus) === true)
					assert (instance.differsFrom (changedTimestamps) === false)
				}
			}

		"support higher-kinded 'unless'" in {
			forAll {
				instance : StorageFacility =>
					val populated = instance.unless (_.name.value.isEmpty) (_.id)
					val empty = instance.unless[Unit] (_.name.value.nonEmpty) {
						_ => fail ("should never be evaluated")
						}

					assert (populated.isDefined)
					assert (empty.isEmpty)
					assert (
						populated.exists (_ === StorageFacility.id.get (instance))
						)
			}
		}

		"support higher-kinded 'when'" in {
			forAll {
				instance : StorageFacility =>
					val populated = instance.when (_.id.toUrn ().nonEmpty) (
						_.version
						)

					val empty = instance.when[Int] (_.name.value.isEmpty) {
						_ => fail ("should never be evaluated")
						}

					assert (populated.isDefined)
					assert (empty.isEmpty)
					assert (
						populated.exists (_ === StorageFacility.version.get (instance))
						)
			}
		}

		"be able to 'touch' version and modification times" in {
			import StorageFacility.{
				id,
				timestamps,
				version
				}


			forAll {
				instance : StorageFacility =>
					val touched = instance.touch[ErrorOr] ()
						.orFail ()

					assert (instance !== touched)
					assert (instance.differsFrom (touched) === true)
					assert (id.get (instance) === id.get (touched))
					assert (
						timestamps.get (instance) !== timestamps.get (touched)
						)

					assert (version.get (instance) !== version.get (touched))
				}
			}

		"detect when one instance differs from another" in {
			forAll {
				(a : StorageFacility, b : StorageFacility) =>
					whenever (a.name !== b.name) {
						assert (!a.differsFrom (a))
						assert (a.differsFrom (b))
						}
				}
			}

		"be able to infer profile changed events" in {
			forAll {
				(a : StorageFacility, b : StorageFacility) =>
					whenever (a.name !== b.name) {
						val events = a.infer[StorageFacilityChangeEvents] (b)
							.toEvents ()

						assert (events.nonEmpty)
						}
				}
			}

		"only infer requested domain events" in {
			forAll {
				(a : StorageFacility, b : StorageFacility) =>
					whenever (a.name !== b.name) {
						val events = a.infer[CNil] (b)
							.toEvents ()

						assert (events.isEmpty)
						}
				}
			}

		"determine what region is the single-point-of-truth" in {
			val otherRegion = Region ("another-region")

			forAll {
				instance : StorageFacility =>
					whenever (instance.primary.exists (_ !== otherRegion)) {
						val thisRegion = instance.primary
							.orFail ("primary region is empty")

						val withoutPrimary = StorageFacility.primary
							.replace (None) (instance)

						assert (instance.definedIn (otherRegion) === false)
						assert (instance.definedIn (thisRegion) === true)
						assert (
							instance.definedIn (thisRegion, default = true) === true
							)

						assert (withoutPrimary.definedIn (thisRegion) === true)
						assert (withoutPrimary.definedIn (otherRegion) === true)
						}
				}
			}
		}
}

