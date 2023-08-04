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
	CompanyStatus,
	ScopedEnvironment
	}

import com.github.osxhacker.demo.company.domain.event.AllCompanyEvents


/**
 * The '''ChangeCompanyStatusSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.company.domain.scenario.ChangeCompanyStatus]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class ChangeCompanyStatusSpec ()
	extends ScenarioSpec ()
		with Diagrams
		with EmitEvents[ScopedEnvironment[IO], AllCompanyEvents]
{
	/// Class Imports
	import cats.syntax.applicative._


	/// Instance Properties
	private lazy val create = CreateCompany[IO] (
		Company.slug
			.andThen (Slug.value)
		) (factory)

	private val factory = Kleisli[ErrorOr, Company, Company] (_.pure[ErrorOr])
	private val scenario = ChangeCompanyStatus[IO] ()


	"The ChangeCompany use-case scenario" must {
		"be able to alter an existing company" in {
			implicit env =>
				val result = for {
					original <- create (createArbitrary[Company] ())
					updated <- scenario (
						original,
						original.version,
						CompanyStatus.Suspended
						)
					} yield {
						assert (original.differsFrom (updated))

						(original, updated)
						}

				result map {
					case (from, to) =>
						assert (Company.id.get (from) === Company.id.get (to))
						assert (
							Company.version.get (from) < Company.version.get (to)
						)

						assert (Company.slug.get (from) === Company.slug.get (to))
						assert (
							Company.version.get (from) !== Company.version.get (to)
							)
					}
			}

		"gracefully reject changing an unknown company" in {
			implicit env =>
				val company = createArbitrary[Company] ()
				val result = scenario (
					company,
					company.version,
					CompanyStatus.Inactive
					)
					.attempt

				result map {
					case Left (error) =>
						assert (error ne null)

					case Right (_) =>
						fail ("expected the change invocation to fail")
					}
			}
		}
}

