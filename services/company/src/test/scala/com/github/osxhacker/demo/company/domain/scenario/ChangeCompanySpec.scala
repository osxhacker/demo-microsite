package com.github.osxhacker.demo.company.domain.scenario

import cats.data.Kleisli
import cats.effect.IO
import eu.timepit.refined
import org.scalatest.diagrams.Diagrams

import com.github.osxhacker.demo.chassis.domain.{
	ErrorOr,
	Slug
	}

import com.github.osxhacker.demo.chassis.domain.entity.Version
import com.github.osxhacker.demo.chassis.domain.event.EmitEvents
import com.github.osxhacker.demo.company.domain.{
	Company,
	ScopedEnvironment
	}

import com.github.osxhacker.demo.company.domain.event.AllCompanyEvents


/**
 * The '''ChangeCompanySpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.company.domain.scenario.ChangeCompany]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class ChangeCompanySpec ()
	extends ScenarioSpec ()
		with Diagrams
		with EmitEvents[ScopedEnvironment[IO], AllCompanyEvents]
{
	/// Class Imports
	import cats.syntax.applicative._
	import refined.auto._


	/// Instance Properties
	private lazy val create = CreateCompany[IO] (slugLens) (factory)
	private lazy val scenario = ChangeCompany[IO] (
		Company.id,
		versionLens,
		slugLens,
		Company.status
		) (factory)

	private val factory = Kleisli[ErrorOr, Company, Company] (_ .pure[ErrorOr])
	private val slugLens = Company.slug
		.asGetter
		.andThen (Slug.value)

	private val versionLens = Company.version
		.asGetter
		.andThen (Version.value)


	"The ChangeCompany use-case scenario" must {
		"be able to alter an existing company" in {
			implicit env =>
				val result = for {
					original <- create (createArbitrary[Company] ())
					updated <- scenario (
						original,
						Company.name
							.replace ("A New Test Name") (original)
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
						assert (Company.name.get (from) !== Company.name.get (to))
					}
			}

		"gracefully reject changing an unknown company" in {
			implicit env =>
				val company = createArbitrary[Company] ()
				val result = scenario (
					company,
					Company.description
						.replace ("This is just a test...  beep!") (company)
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

