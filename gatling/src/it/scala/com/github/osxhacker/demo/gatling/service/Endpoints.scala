package com.github.osxhacker.demo.gatling.service

import com.github.osxhacker.demo.gatling.ServiceEndpoint


/**
 * The '''Endpoints''' `object` defines the known microservices and their
 * default [[com.github.osxhacker.demo.gatling.ServiceEndpoint]]s.
 */
object Endpoints
{
	/// Instance Properties
	val company = ServiceEndpoint ("service.company", "http://localhost:6891")
	val storageFacility = ServiceEndpoint (
		"service.storageFacility",
		"http://localhost:6890"
		)
}
