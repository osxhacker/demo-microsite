package com.github.osxhacker.demo.storageFacility.domain.scenario

import cats.effect.IO
import org.scalatest.diagrams.Diagrams

import com.github.osxhacker.demo.storageFacility.domain.Company
import com.github.osxhacker.demo.storageFacility.domain.event._


/**
 * The '''InterpretCompanyEventsSpec''' type defines the unit-tests which
 * certify
 * [[com.github.osxhacker.demo.storageFacility.domain.scenario.InterpretCompanyEvents]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class InterpretCompanyEventsSpec
	extends ScenarioSpec ()
		with Diagrams
{
	/// Class Imports
	import shapeless.syntax.inject._


	"The InterpretCompanyEvents use-case scenario" must {
		"only require domain types" in {
			_ =>
				InterpretCompanyEvents[IO] ()

				succeed
			}

		"be able to interpret 'CompanyCreated' events" in {
			implicit env =>
				val global = createGlobalEnvironment (env)
				val company = createArbitrary[Company] ()
				val interpreter = InterpretCompanyEvents[IO] ()

				val result = interpreter () (
					CompanyCreated (
						region = env.region,
						fingerprint = None,
						correlationId = env.correlationId,
						id = company.id,
						slug = company.slug,
						name = company.name,
						status = company.status,
						timestamps = company.timestamps
						)
						.inject[AllCompanyEvents] -> global
					)

				result as succeed
			}

		"handle duplicate 'CompanyCreated' events" in {
			implicit env =>
				val global = createGlobalEnvironment (env)
				val company = createArbitrary[Company]()
				val interpreter = InterpretCompanyEvents[IO]()

				val event = CompanyCreated (
					region = env.region,
					fingerprint = None,
					correlationId = env.correlationId,
					id = company.id,
					slug = company.slug,
					name = company.name,
					status = company.status,
					timestamps = company.timestamps
					)
					.inject[AllCompanyEvents]

				val result = interpreter () (event -> global) >>
					interpreter ()(event -> global)

				result as succeed
			}

		"be able to interpret 'CompanyDeleted' events" in {
			implicit env =>
				val global = createGlobalEnvironment (env)
				val company = createArbitrary[Company] ()
				val interpreter = InterpretCompanyEvents[IO] ()

				val result = interpreter () (
					CompanyCreated (
						region = env.region,
						fingerprint = None,
						correlationId = env.correlationId,
						id = company.id,
						slug = company.slug,
						name = company.name,
						status = company.status,
						timestamps = company.timestamps
						)
						.inject[AllCompanyEvents] -> global
					) >>
					interpreter () (
						CompanyDeleted (
							region = env.region,
							fingerprint = None,
							correlationId = env.correlationId,
							id = company.id
							)
							.inject[AllCompanyEvents] -> global
						)

				result as succeed
			}

		"handle a 'CompanyDeleted' event when the company does not exist" in {
			implicit env =>
				val global = createGlobalEnvironment (env)
				val company = createArbitrary[Company] ()
				val interpreter = InterpretCompanyEvents[IO] ()

				val result = interpreter () (
					CompanyDeleted (
						region = env.region,
						fingerprint = None,
						correlationId = env.correlationId,
						id = company.id
						)
						.inject[AllCompanyEvents] -> global
					)

				result as succeed
			}
		}
}

