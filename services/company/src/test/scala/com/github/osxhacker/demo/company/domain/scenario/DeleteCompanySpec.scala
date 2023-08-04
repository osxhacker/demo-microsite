package com.github.osxhacker.demo.company.domain.scenario

import cats.effect.IO
import org.scalatest.diagrams.Diagrams

import com.github.osxhacker.demo.chassis.domain.event.EmitEvents
import com.github.osxhacker.demo.chassis.domain.repository.CreateIntent
import com.github.osxhacker.demo.company.domain.{
	Company,
	ScopedEnvironment
	}

import com.github.osxhacker.demo.company.domain.event.AllCompanyEvents


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
					saved <- save (CreateIntent (createArbitrary[Company] ()))
						.map (_.orFail ("save did not produce a result"))

					instance <- find (saved.id)
					answer <- scenario (instance)
					} yield answer

				result map {
					yesOrNo =>
						assert (yesOrNo === true)

						inspectEvents (env.companyEvents) {
							emitted =>
								assert (emitted.nonEmpty)
							}
					}
			}

		"gracefully handle when the company does not exist" in {
			implicit env =>
				scenario (createArbitrary[Company] ()) map {
					yesOrNo =>
						assert (yesOrNo === false)

						inspectEvents (env.companyEvents) {
							emitted =>
								assert (emitted.isEmpty)
							}
					}
			}

		"reject deleting a company with a reserved slug" in {
			implicit env =>
				val hasReservedSlug = Company.slug
					.replace (env.reservedSlugs.head) (
						createArbitrary[Company] ()
						)

				scenario (hasReservedSlug).attempt map {
					case Left (error) =>
						assert (error.getMessage.nonEmpty)

					case Right (_) =>
						fail ("expected delete with reserved slug to fail")
					}
			}
		}
}
