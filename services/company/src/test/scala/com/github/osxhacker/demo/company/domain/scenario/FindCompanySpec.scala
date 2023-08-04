package com.github.osxhacker.demo.company.domain.scenario

import cats.effect.IO
import org.scalatest.diagrams.Diagrams

import com.github.osxhacker.demo.chassis.domain.entity.Version
import com.github.osxhacker.demo.chassis.domain.error.ObjectNotFoundError
import com.github.osxhacker.demo.chassis.domain.repository.CreateIntent
import com.github.osxhacker.demo.company.domain.Company


/**
 * The '''FindCompanySpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.company.domain.scenario.FindCompany]]
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
		"be able to find an existing company" in {
			implicit env =>
				val result = for {
					saved <- save (CreateIntent (createArbitrary[Company] ()))
						.map (_.orFail ("save did not produce a result"))

					answer <- scenario (saved.id)
					} yield answer

				result map {
					instance =>
						assert (instance.version >= Version.initial)
				}
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

