package com.github.osxhacker.demo.storageFacility.domain.scenario

import cats.effect.IO
import org.scalatest.diagrams.Diagrams

import com.github.osxhacker.demo.chassis.domain.error.ObjectNotFoundError
import com.github.osxhacker.demo.chassis.domain.repository.CreateIntent
import com.github.osxhacker.demo.storageFacility.domain.{
	Company,
	CompanyReference
	}


/**
 * The '''FindCompanySpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.storageFacility.domain.scenario.FindCompany]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class FindCompanySpec ()
	extends ScenarioSpec ()
		with Diagrams
{
	/// Instance Properties
	private val save = SaveCompany[IO] ()
	private val scenario = FindCompany[IO] ()


	"The FindFacility scenario" must {
		"be able to find an existing company using its identifier" in {
			implicit env =>
				for {
					saved <- save[CreateIntent] (createArbitrary[Company] ())
					_ <- scenario (saved.id)
					} yield succeed
			}

		"be able to find an existing instance using a company reference" in {
			implicit env =>
				for {
					saved <- save[CreateIntent] (createArbitrary[Company]())
					_ <- scenario (CompanyReference (saved.slug))
					} yield succeed
			}

		"fail when the company does not exist" in {
			implicit env =>
				scenario (createArbitrary[Company] ().id).map (_ => fail ())
					.recover {
						case _ : ObjectNotFoundError[_] =>
							succeed
						}
			}
		}
}

