package com.github.osxhacker.demo.company.adapter.rest.arrow

import eu.timepit.refined
import org.scalacheck.{
	Arbitrary,
	Gen
	}

import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import shapeless.tag

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.chassis.adapter.rest.{
	Path,
	ResourceLocation
	}

import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.company.adapter.rest.ApiSupport
import com.github.osxhacker.demo.company.adapter.rest.api
import com.github.osxhacker.demo.company.domain
import com.github.osxhacker.demo.company.domain.CompanySupport


/**
 * The '''CompaniesToApiSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.company.adapter.rest.arrow.CompaniesToApi]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class CompaniesToApiSpec ()
	extends AnyWordSpec
		with Diagrams
		with ScalaCheckPropertyChecks
		with ProjectSpec
		with ApiSupport
		with CompanySupport
{
	/// Class Imports
	import cats.syntax.flatMap._
	import cats.syntax.show._
	import refined.auto._


	/// Instance Properties
	private lazy val mockResourceLocation = tag[api.Companies] (
		ResourceLocation (Path ("/test"))
		)

	implicit private lazy val multipleCompanies = Arbitrary {
		Gen.choose (1, 50)
			.flatMap {
				n =>
					Gen.buildableOfN[
						Vector[domain.Company],
						domain.Company
						] (
						n,
						implicitly[Arbitrary[domain.Company]].arbitrary
						)
				}
		}


	"The CompaniesToApi arrow" must {
		"support vector of Company types" in {
			forAll {
				source : Vector[domain.Company] =>
					val arrow = CompaniesToApi[ErrorOr] ()
					val result = arrow ().run (source -> mockResourceLocation)

					assert (result.isRight)
					result foreach {
						resource =>
							val links = resource._links >>= {
								_.additionalProperties
								}

							assert (links.isDefined)
							assert (links.exists (_.self.isDefined))
					}

					result.map (_.companies) foreach {
						companies =>
							assert (companies.size === source.size)

							companies foreach {
								company =>
									val links = company._links >>= {
										_.additionalProperties
										}

									assert (links.nonEmpty)
									assert (links.exists (_.self.isDefined))
									assert (links.exists (_.edit.isDefined))
								}
						}
				}
			}
		}
}

