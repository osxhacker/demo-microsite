package com.github.osxhacker.demo.gatling.service.storageFacility

import java.util.UUID.randomUUID

import scala.concurrent.duration._
import scala.language.postfixOps

import com.github.osxhacker.demo.api.storageFacility.{
	NewStorageFacility,
	StorageFacilityStatus
	}

import com.github.osxhacker.demo.gatling.LoadTestSimulation


/**
 * The '''StorageFacilityLoadTest''' type defines a
 * [[com.github.osxhacker.demo.gatling.LoadTestSimulation]] which exercises the
 * Storage Facility micro-service.  Testing parameters are tunable by way of JVM
 * system properties described below.
 */
final class StorageFacilityLoadTest ()
	extends LoadTestSimulation ("http://localhost:6890")
{
	/// Class Types
	private object tasks
	{
		lazy val create = task.CreateStorageFacility (serviceEndpoint)
		lazy val findAll = task.FindAllStorageFacilities (serviceEndpoint)
		lazy val initialize = task.InitializeSystem (
			serviceEndpoint,
			generatedSlug
			)

		lazy val owner = task.ResolveOwningCompany (serviceEndpoint)
	}


	/// Instance Properties
	override protected lazy val initialize =
		scenario ("Initialize Micro-Services").exec (tasks.initialize ())

	override protected lazy val loadTest =
		scenario ("Storage Facility Load Test")
			.exec (tasks.owner (generatedSlug))
			.exec (tasks.findAll (minimumExisting = 0))
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
			.exec (tasks.findAll (minimumExisting = 0))

	private val generatedSlug = s"gatling-${System.currentTimeMillis}"


	/// Constructor Body
	evaluate (protocols.json) {
		settings.delay :::
		settings.burstUsers :::
		settings.constant :::
		settings.ramp
		}
}
