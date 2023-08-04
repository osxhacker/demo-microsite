package com.github.osxhacker.demo.company.domain.scenario

import cats.data.Kleisli
import cats.effect.IO
import org.scalatest.diagrams.Diagrams

import com.github.osxhacker.demo.chassis.domain.{
	ErrorOr,
	Slug
	}

import com.github.osxhacker.demo.chassis.domain.event.EmitEvents
import com.github.osxhacker.demo.company.domain.{
	Company,
	ScopedEnvironment
	}

import com.github.osxhacker.demo.company.domain.event.AllCompanyEvents


/**
 * The '''CreateCompanySpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.company.domain.scenario.CreateCompany]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class CreateCompanySpec ()
	extends ScenarioSpec ()
		with Diagrams
		with EmitEvents[ScopedEnvironment[IO], AllCompanyEvents]
{
	/// Class Imports
	import cats.syntax.applicative._


	/// Instance Properties
	private val factory = Kleisli[ErrorOr, Company, Company] (_.pure[ErrorOr])
	private val scenario = CreateCompany[IO] (
		Company.slug
			.andThen (Slug.value)
		) (factory)


	"The CreateCompany use-case scenario" must {
		"be able to create a previously unknown company" in {
			implicit env =>
				val company = createArbitrary[Company] ()
				val result = scenario (company)

				result map {
					saved =>
						assert (saved.slug === company.slug)

						inspectEvents (env.companyEvents) {
							emitted =>
								assert (emitted.nonEmpty)
								assert (
									emitted.exists (_.isInstanceOf[AllCompanyEvents])
									)
							}
					}
			}

		"reject duplicate companies" in {
			implicit env =>
				val company = createArbitrary[Company] ()
				val result = scenario (company) >> scenario (company)

				result.attempt map {
					case Right (_) =>
						fail ("expected duplicate create to fail")

					case Left (failure) =>
						assert (failure.getMessage ne null)

						inspectEvents (env.companyEvents) {
							emitted =>
								assert (emitted.size === 1)
								assert (
									emitted.exists (_.isInstanceOf[AllCompanyEvents])
									)
							}
					}
			}
		}
}

