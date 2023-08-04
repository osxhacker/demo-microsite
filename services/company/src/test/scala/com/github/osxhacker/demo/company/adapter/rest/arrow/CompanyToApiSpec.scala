package com.github.osxhacker.demo.company.adapter.rest.arrow

import eu.timepit.refined
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
import com.github.osxhacker.demo.chassis.domain.entity.Version
import com.github.osxhacker.demo.company.adapter.rest.ApiSupport
import com.github.osxhacker.demo.company.adapter.rest.api
import com.github.osxhacker.demo.company.domain
import com.github.osxhacker.demo.company.domain.specification.CompanyStatusIs


/**
 * The '''CompanyToApiSpec ''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.company.adapter.rest.arrow.CompanyToApi]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class CompanyToApiSpec ()
	extends AnyWordSpec
		with Diagrams
		with ScalaCheckPropertyChecks
		with ProjectSpec
		with ApiSupport
		with domain.CompanySupport
{
	/// Class Imports
	import cats.syntax.flatMap._
	import cats.syntax.show._
	import refined.auto._


	/// Instance Properties
	private lazy val mockResourceLocation = tag[api.Company] (
		ResourceLocation (Path ("/test"))
		)


	"The CompanyToApi arrow" must {
		"support Company types" in {
			forAll {
				source : domain.Company =>
					val arrow = CompanyToApi[ErrorOr] ()
					val result = arrow ().run (source -> mockResourceLocation)

					assert (CompanyStatusIs (domain.CompanyStatus.Active) (source))
					assert (result.isRight)
					result foreach {
						resource =>
							val links = resource._links >>= {
								_.additionalProperties
								}

							assert (resource._links.isDefined)
							assert (links.exists (_.nonEmpty))
							assert (
								links.exists (_.`urn:company:deactivate`.isDefined)
								)

							assert (resource.id.value === source.id.show)
							assert (
								resource.version === Version.value.get (source.version)
								)

							assert (resource.name.value === source.name.value)
							assert (
								resource.description.value === source.description.value
								)
						}
					}
				}
		}
}

