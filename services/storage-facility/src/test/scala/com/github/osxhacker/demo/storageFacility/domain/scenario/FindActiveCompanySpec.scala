package com.github.osxhacker.demo.storageFacility.domain.scenario


import cats.effect.IO
import org.scalatest.Assertion
import org.scalatest.diagrams.Diagrams

import com.github.osxhacker.demo.chassis.domain.error.ObjectNotFoundError
import com.github.osxhacker.demo.chassis.domain.repository.CreateIntent
import com.github.osxhacker.demo.storageFacility.domain.{
	Company,
	CompanyReference,
	CompanyStatus
	}


/**
 * The '''FindActiveCompanySpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.storageFacility.domain.scenario.FindActiveCompany]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class FindActiveCompanySpec
	extends ScenarioSpec ()
		with Diagrams
{
	/// Instance Properties
	private val save = SaveCompany[IO] ()
	private val scenario = FindCompany[IO] ()


	"The FindFacility scenario" must {
		"be able to find an existing active company" in {
			implicit env =>
				for {
					saved <- save[CreateIntent] (createActiveCompany ())
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

		"fail when an existing company is not active" in {
			implicit env =>
				for {
					saved <- save[CreateIntent] (createSuspendedCompany ())
					result <- scenario (CompanyReference (saved.slug)).map (
						_ => succeed
						)
						.handleError (_ => succeed)
					} yield result
			}
		}


	private def createActiveCompany () : Company =
		Company.status
			.replace (CompanyStatus.Active) (createArbitrary[Company] ())


	private def createSuspendedCompany () : Company =
		Company.status
			.replace (CompanyStatus.Suspended) (createArbitrary[Company] ())
}

