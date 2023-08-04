package com.github.osxhacker.demo.storageFacility.adapter.rest.arrow

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
import com.github.osxhacker.demo.storageFacility.adapter.rest.ApiSupport
import com.github.osxhacker.demo.storageFacility.adapter.rest.api
import com.github.osxhacker.demo.storageFacility.domain
import com.github.osxhacker.demo.storageFacility.domain.StorageFacilitySupport


/**
 * The '''FacilitiesToApiSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.arrow.FacilitiesToApi]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class FacilitiesToApiSpec ()
	extends AnyWordSpec
		with Diagrams
		with ScalaCheckPropertyChecks
		with ProjectSpec
		with ApiSupport
		with StorageFacilitySupport
{
	/// Class Imports
	import cats.syntax.flatMap._
	import cats.syntax.show._
	import refined.auto._


	/// Instance Properties
	private lazy val mockResourceLocation = tag[api.StorageFacilities] (
		ResourceLocation (Path ("/test"))
		)

	implicit private lazy val multipleFacilities = Arbitrary {
		Gen.choose (1, 50)
			.flatMap {
				n =>
					Gen.buildableOfN[
						Vector[domain.StorageFacility],
						domain.StorageFacility
						] (
						n,
						implicitly[Arbitrary[domain.StorageFacility]].arbitrary
						)
				}
		}


	"The FacilitiesToApi arrow" must {
		"support vector of StorageFacility types" in {
			forAll {
				source : Vector[domain.StorageFacility] =>
					val arrow = FacilitiesToApi[ErrorOr] ()
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

					result.map (_.facilities) foreach {
						facilities =>
							assert (facilities.size === source.size)

							facilities foreach {
								facility =>
									val links = facility._links >>= {
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

