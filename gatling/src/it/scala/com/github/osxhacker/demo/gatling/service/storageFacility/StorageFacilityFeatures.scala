package com.github.osxhacker.demo.gatling.service.storageFacility

import java.util.UUID.randomUUID

import scala.concurrent.duration._
import scala.language.postfixOps

import com.github.osxhacker.demo.gatling.FeatureSimulation
import com.github.osxhacker.demo.api.storageFacility.{
	NewStorageFacility,
	StorageFacilityStatus
	}


/**
 * The '''StorageFacilityFeatures''' type is a minimal
 * [[com.github.osxhacker.demo.gatling.FeatureSimulation]] intended to verify
 * functionality of the Storage Facility service using
 * [[https://gatling.io/docs/gatling/tutorials/quickstart/ Gatling]].
 */
final class StorageFacilityFeatures ()
	extends FeatureSimulation ("http://localhost:6890")
{
	/// Class Types
	private object tasks
	{
		lazy val activate = task.ActivateStorageFacility (serviceEndpoint)
		lazy val close = task.CloseStorageFacility (serviceEndpoint)
		lazy val create = task.CreateStorageFacility (serviceEndpoint)
		lazy val initialize = task.InitializeSystem (
			serviceEndpoint,
			generatedSlug
			)

		lazy val invalidCreations = task.DetectInvalidCreations (create)
		lazy val owner = task.ResolveOwningCompany (serviceEndpoint)
	}


	/// Instance Properties
	private lazy val addFacility =
		scenario ("Create New Storage Facility").exec (
			logSession ("Start 'create then close' scenario:")
			)
			.exec (tasks.owner (generatedSlug))
			.exec (
				tasks.create (
					NewStorageFacility (
						name = s"Gatling - ${randomUUID ()}",
						status = StorageFacilityStatus.Active,
						city = "Chicago",
						state = "IL",
						zip = "60604",
						capacity = 100_000,
						available = 100_000
						)
					)
				)
			.exec (tasks.close ())
			.exec (tasks.activate ())
			.exec (logSession ("End 'create then close' scenario:"))

	private lazy val failedAdds =
		scenario ("Reject Invalid Storage Facility Adds").exec (
			logSession ("Start 'failed adds' scenario:")
			)
			.exec (tasks.owner (generatedSlug))
			.exec (tasks.invalidCreations ())
			.exec (logSession ("End 'failed adds' scenario:"))

	private lazy val initialize = scenario ("Initialize System").exec (
		tasks.initialize ()
		)
		// allow domain events to propagate
		.pause (200 milliseconds, 500 milliseconds)

	private val generatedSlug = s"gatling-${randomUUID ()}"


	/// Constructor Body
	evaluate (protocols.json) {
		initialize ::
		addFacility ::
		failedAdds ::
		Nil
		}
}

