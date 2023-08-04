package com.github.osxhacker.demo.company.domain.scenario

import cats.effect.IO
import org.scalatest.diagrams.Diagrams

import com.github.osxhacker.demo.company.domain.Company
import com.github.osxhacker.demo.company.domain.event._


/**
 * The '''InterpretCompanyEventsSpec''' type defines the unit-tests which
 * certify
 * [[com.github.osxhacker.demo.company.domain.scenario.InterpretCompanyEvents]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class InterpretCompanyEventsSpec
	extends ScenarioSpec ()
		with Diagrams
{
	/// Class Imports
	import shapeless.syntax.inject._


	/// Instance Properties
	private val global = createGlobalEnvironment ()


	"The InterpretCompanyEvents use-case scenario" must {
		"only require domain types" in {
			_ =>
				InterpretCompanyEvents[IO] ()

				succeed
			}

		"be able to interpret 'CompanyCreated' events" in {
			implicit env =>
				val company = createArbitrary[Company] ()
				val interpreter = InterpretCompanyEvents[IO] ()

				val result = interpreter () (
					CompanyCreated (company).inject[AllCompanyEvents] -> global
					)

				result as succeed
			}

		"handle duplicate 'CompanyCreated' events" in {
			implicit env =>
				val company = createArbitrary[Company] ()
				val interpreter = InterpretCompanyEvents[IO] ()

				val event = CompanyCreated (company).inject[AllCompanyEvents]
				val result = interpreter () (event -> global) >>
					interpreter () (event -> global)

				result as succeed
			}

		"be able to interpret 'CompanyDeleted' events" in {
			implicit env =>
				val company = createArbitrary[Company] ()
				val interpreter = InterpretCompanyEvents[IO] ()

				val result = interpreter () (
					CompanyCreated (company).inject[AllCompanyEvents] -> global
					) >>
					interpreter () (
						CompanyDeleted (company).inject[AllCompanyEvents] -> global
						)

				result as succeed
			}

		"handle a 'CompanyDeleted' event when the company does not exist" in {
			implicit env =>
				val company = createArbitrary[Company] ()
				val interpreter = InterpretCompanyEvents[IO] ()

				val result = interpreter () (
					CompanyDeleted (company).inject[AllCompanyEvents] -> global
					)

				result as succeed
			}
		}
}

