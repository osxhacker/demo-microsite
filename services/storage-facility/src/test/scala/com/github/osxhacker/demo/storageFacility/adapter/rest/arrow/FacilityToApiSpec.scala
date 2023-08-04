package com.github.osxhacker.demo.storageFacility.adapter.rest.arrow

import eu.timepit.refined
import io.scalaland.chimney
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
import com.github.osxhacker.demo.storageFacility.adapter.rest.ApiSupport
import com.github.osxhacker.demo.storageFacility.adapter.rest.api
import com.github.osxhacker.demo.storageFacility.domain
import com.github.osxhacker.demo.storageFacility.domain.StorageFacilitySupport


/**
 * The '''FacilityToApiSpec ''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.arrow.FacilityToApi]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class FacilityToApiSpec ()
	extends AnyWordSpec
		with Diagrams
		with ScalaCheckPropertyChecks
		with ProjectSpec
		with ApiSupport
		with StorageFacilitySupport
{
	/// Class Imports
	import cats.syntax.either._
	import cats.syntax.flatMap._
	import cats.syntax.option._
	import cats.syntax.show._
	import chimney.cats._
	import chimney.dsl._
	import domain.transformers._
	import refined.auto._


	/// Instance Properties
	private lazy val mockResourceLocation = tag[api.StorageFacility] (
		ResourceLocation (Path ("/test"))
		)


	"The FacilityToApi arrow" must {
		"support StorageFacility types" in {
			forAll {
				source : domain.StorageFacility =>
					val arrow = FacilityToApi[ErrorOr] ()
					val result = arrow ().run (source -> mockResourceLocation)

					assert (result.isRight)
					result foreach {
						resource =>
							val links = resource._links >>= {
								_.additionalProperties
								}

							assert (resource._links.isDefined)
							assert (links.exists (_.nonEmpty))
							assert (
								links.exists (_.`urn:storage-facility:close`.isDefined)
								)

							assert (resource.id.value === source.id.show)
							assert (
								resource.version === Version.value.get (source.version)
								)

							assert (resource.name.value === source.name.value)
							assert (resource.city.value === source.city.value)
							assert (resource.state.value === source.state.value)
							assert (resource.zip.value === source.zip.value)
							assert (
								resource.capacity.value === source.capacity.value
								)

							assert (
								resource.available.value === source.available.value
								)
						}
					}
				}

		"support embedding owning company within StorageFacility" in {
			forAll {
				source : domain.StorageFacility =>
					val arrow = FacilityToApi[ErrorOr] ()
					val expander = ExpandFacilityCompany[ErrorOr] ()
					val result = arrow (expander ().map (_._2)).run (
						source -> mockResourceLocation
						)

					assert (result.isRight)
					result foreach {
						resource =>
							val company = resource._embedded
								.flatMap (_.values.get ("company"))

							assert (resource._embedded.isDefined)
							assert (company.isDefined)

							company match {
								case Some (view : api.StorageFacilityCompanyView) =>
									assert (view.id.value === source.owner.id.show)

								case other =>
									fail (s"did not find company view: $other")
								}
							}
				}
			}
		}
}

