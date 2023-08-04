package com.github.osxhacker.demo.storageFacility.domain.scenario

import cats.effect.IO
import org.scalatest.diagrams.Diagrams

import com.github.osxhacker.demo.chassis.domain.event.EmitEvents
import com.github.osxhacker.demo.chassis.domain.repository.CreateIntent
import com.github.osxhacker.demo.storageFacility.domain.{
	Company,
	ScopedEnvironment
	}

import com.github.osxhacker.demo.storageFacility.domain.event.AllCompanyEvents


/**
 * The '''DeleteCompanySpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.company.domain.scenario.DeleteCompany]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class DeleteCompanySpec ()
	extends ScenarioSpec ()
		with Diagrams
		with EmitEvents[ScopedEnvironment[IO], AllCompanyEvents]
{
	/// Instance Properties
	private val find = FindCompany[IO] ()
	private val save = SaveCompany[IO] ()
	private val scenario = DeleteCompany[IO] ()


	"The DeleteCompany use-case scenario" must {
		"be able to delete an existing company" in {
			implicit env =>
				val result = for {
					saved <- save[CreateIntent] (createArbitrary[Company] ())
					instance <- find (saved.id)
					answer <- scenario (instance)
					} yield answer

				result map {
					yesOrNo =>
						assert (yesOrNo === true)
					}
			}

		"detect when the company does not exist" in {
			implicit env =>
				scenario (createArbitrary[Company] ()).attempt
					.map (_.fold (_ => succeed, _ => fail ()))
			}
		}
}
