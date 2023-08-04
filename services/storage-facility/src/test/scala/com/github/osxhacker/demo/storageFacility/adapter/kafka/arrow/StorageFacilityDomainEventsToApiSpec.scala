package com.github.osxhacker.demo.storageFacility.adapter.kafka.arrow

import java.util.UUID.randomUUID

import eu.timepit.refined
import org.scalatest.Assertion
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec
import org.typelevel.log4cats.noop.NoOpFactory
import shapeless.{
	syntax => _,
	_
	}

import shapeless.ops.coproduct.Inject

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.chassis.domain.entity.Identifier
import com.github.osxhacker.demo.chassis.domain.event.{
	EventSupport,
	Region
	}

import com.github.osxhacker.demo.chassis.monitoring.{
	CorrelationId,
	Subsystem
	}

import com.github.osxhacker.demo.storageFacility.adapter.rest.api
import com.github.osxhacker.demo.storageFacility.adapter.database.{
	AlwaysFailCompanyRepository,
	AlwaysFailStorageFacilityRepository
	}

import com.github.osxhacker.demo.storageFacility.domain._
import com.github.osxhacker.demo.storageFacility.domain.event._


/**
 * The '''StorageFacilityDomainEventsToApiSpec''' type defines the unit-tests
 * which certify
 * [[com.github.osxhacker.demo.storageFacility.adapter.kafka.arrow.StorageFacilityDomainEventsToApi]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class StorageFacilityDomainEventsToApiSpec ()
	extends AnyWordSpec
		with Diagrams
		with ProjectSpec
		with StorageFacilitySupport
		with EventSupport
{
	/// Class Imports
	import cats.syntax.show._
	import refined.auto._
	import refined.cats._
	import shapeless.syntax.inject._


	/// Instance Properties
	implicit protected val loggerFactory = NoOpFactory[ErrorOr]
	implicit protected def subsystem = Subsystem ("unit-test")
	implicit lazy val env = GlobalEnvironment[ErrorOr] (
		Region ("unit-test"),
		CompanyReference (Identifier.fromRandom[Company] ()),
		AlwaysFailCompanyRepository[ErrorOr] (new RuntimeException()),
		AlwaysFailStorageFacilityRepository[ErrorOr] (
			new RuntimeException ()
			),

		new MockEventProducer[ErrorOr, EventChannel, AllStorageFacilityEvents] (
			EventChannel.UnitTest
			)
		)
		.scopeWith (
			CompanyReference (Identifier.fromRandom[Company] ()),
			CorrelationId[ErrorOr] (randomUUID ()).orFail ()
			)
		.orFail ()


	"The StorageFacilityDomainEventsToApi arrow" must {
		"be able to produce a StorageFacilityCreated-based event" in {
			checkApiEvent (StorageFacilityCreated (_))
			}

		"be able to produce a StorageFacilityDeleted-based event" in {
			checkApiEvent (StorageFacilityDeleted (_))
			}

		"be able to produce a StorageFacilityProfileChanged-based event" in {
			checkApiEvent (StorageFacilityProfileChanged (_))
			}

		"be able to produce a StorageFacilityStatusChanged-based event" in {
			checkApiEvent (StorageFacilityStatusChanged (_))
			}
		}


	private def checkApiEvent[EventT <: StorageFacilityEvent] (
		f : StorageFacility => EventT
		)
		(implicit inject : Inject[AllStorageFacilityEvents, EventT])
		: Assertion =
	{
		val facility = createArbitrary[StorageFacility] ()
		val event = f (facility)
		val result = StorageFacilityDomainEventsToApi (inject (event))

		assert (result.isRight)
		result foreach {
			instance =>
				assert (
					instance.origin.correlationId.show === env.correlationId.show
					)

				/// The `origin.id` identifies the event itself, not the
				/// entity for which an event was emitted.
				assert (instance.origin.id !== event.id.show)
				assert (instance.origin.region.show === event.region.show)

				/// A domain event is always regarding an entity
				/// identifiable by a well-known URN.
				assert (instance.id.show === event.id.show)
				assert (instance.owner.show === facility.owner.id.show)
			}

		succeed
	}
}

