package com.github.osxhacker.demo.storageFacility.adapter.rest.arrow

import eu.timepit.refined
import io.scalaland.chimney
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import shapeless.{
	syntax => _,
	_
	}

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.chassis.domain.event.Region
import com.github.osxhacker.demo.storageFacility.adapter.rest.ApiSupport
import com.github.osxhacker.demo.storageFacility.adapter.rest.api
import com.github.osxhacker.demo.storageFacility.domain
import com.github.osxhacker.demo.storageFacility.domain.{
	Company,
	CompanySupport
	}


/**
 * The '''FacilityFromApiSpec ''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.arrow.FacilityFromApi]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class FacilityFromApiSpec ()
	extends AnyWordSpec
		with Diagrams
		with ScalaCheckPropertyChecks
		with ProjectSpec
		with ApiSupport
		with CompanySupport
{
	/// Class Imports
	import chimney.cats._
	import chimney.dsl._
	import domain.transformers._
	import refined.auto._


	/// Instance Property
	private lazy val region = Region ("test" : Region.Value)
	private lazy val tenant = createArbitrary[Company] ()


	"The FacilityFromApi arrow" must {
		"support NewStorageFacility resource types" in {
			forAll {
				source : api.NewStorageFacility =>
					val arrow = FacilityFromApi[api.NewStorageFacility] ()
					val result = arrow ().run (source :: region :: tenant :: HNil)

					assert (result.isRight)
					result foreach {
						mapped =>
							assert (
								mapped.status.toString () === source.status.toString ()
								)

							assert (mapped.available.value === source.available.value)
							assert (mapped.capacity.value === source.capacity.value)
						}
				}
			}

		"support StorageFacility resource types" in {
			forAll {
				source : api.StorageFacility =>
					val arrow = FacilityFromApi[api.StorageFacility] ()
					val result = arrow ().run (source :: region :: tenant :: HNil)

					assert (result.isRight)
					result foreach {
						mapped =>
							assert (
								mapped.status.toString () === source.status.toString ()
								)

							assert (
								mapped.available.value === source.available.value
								)

							assert (
								mapped.capacity.value === source.capacity.value
								)
						}
				}
			}
		}
}
