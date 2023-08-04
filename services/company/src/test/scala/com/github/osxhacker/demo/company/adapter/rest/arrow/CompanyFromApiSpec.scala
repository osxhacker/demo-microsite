package com.github.osxhacker.demo.company.adapter.rest.arrow

import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.company.adapter.rest.ApiSupport
import com.github.osxhacker.demo.company.adapter.rest.api


/**
 * The '''CompanyFromApiSpec ''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.company.adapter.rest.arrow.CompanyFromApi]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class CompanyFromApiSpec ()
	extends AnyWordSpec
		with Diagrams
		with ScalaCheckPropertyChecks
		with ProjectSpec
		with ApiSupport
{
	"The CompanyFromApi arrow" must {
		"support NewCompany resource types" in {
			forAll {
				source : api.NewCompany =>
					val arrow = CompanyFromApi[api.NewCompany] ()
					val result = arrow ().run (source)

					assert (result.isRight)
					result foreach {
						mapped =>
							assert (
								mapped.status.toString () === source.status.toString ()
								)

							assert (mapped.name.value === source.name.value)
							assert (
								mapped.description.value === source.description.value
								)
						}
				}
			}

		"support Company resource types" in {
			forAll {
				source : api.Company =>
					val arrow = CompanyFromApi[api.Company] ()
					val result = arrow ().run (source)

					assert (result.isRight)
					result foreach {
						mapped =>
							assert (
								mapped.status.toString () === source.status.toString ()
								)

							assert (mapped.name.value === source.name.value)
							assert (
								mapped.description.value === source.description.value
								)

						}
				}
			}
		}
}

