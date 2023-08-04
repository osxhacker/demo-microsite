package com.github.osxhacker.demo.company.domain

import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import shapeless.{
	syntax => _,
	_
	}

import com.github.osxhacker.demo.chassis.ProjectSpec


/**
 * The '''CompanyStatusSpec ''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.company.domain.Company]] for
 * fitness of purpose and serves as an exemplar of its use.
 */
final class CompanyStatusSpec ()
	extends AnyWordSpec
		with Diagrams
		with ProjectSpec
		with CompanySupport
		with ScalaCheckPropertyChecks
{
	/// Class Imports
	import CompanyStatusSpec._


	/// Instance Properties
	implicit private val companyStatus = Arbitrary (
		Gen.oneOf (CompanyStatus.values)
		)


	"The CompanyStatus type" must {
		"be a model of 'EnumEntry'" in {
			assert (
				classOf[enumeratum.EnumEntry].isAssignableFrom (classOf[CompanyStatus])
				)
			}

		"support polymorphic functor mapping" in {
			forAll {
				(status : CompanyStatus) =>
					val prefix = "The company status"
					val result = status.pfmap[
						StatusToLowerString.type,
						String,
						String
						] (prefix)

					assert (result.startsWith (prefix))
					assert (result.length > prefix.length)
				}
			}

		"enforce all statuses are accounted for" in {
			assertDoesNotCompile (
				"""
				val status = createArbitrary[CompanyStatus] ()

				status.pfmap[MissingAStatus.type, Int, String] (3)
				"""
				)
			}
		}
}


object CompanyStatusSpec
{
	/// Class Types
	object MissingAStatus
		extends Poly2
	{
		/// Class Imports
		import CompanyStatus._


		/// Implicit Conversions
		implicit val caseActive : Case.Aux[Active.type, Int, String] =
			at[Active.type, Int] {
				(active, repeat) =>
					active.entryName * repeat
				}

		implicit val caseInactive : Case.Aux[Inactive.type, Int, String] =
			at[Inactive.type, Int] {
				case (inactive, repeat) =>
					inactive.entryName * repeat
				}
	}


	object StatusToLowerString
		extends Poly2
	{
		/// Class Imports
		import CompanyStatus._


		/// Implicit Conversions
		implicit val caseActive : Case.Aux[Active.type, String, String] =
			at[Active.type, String] {
				(active, prefix) =>
					prefix + ": " + active.entryName.toLowerCase
				}

		implicit val caseInactive : Case.Aux[Inactive.type, String, String] =
			at[Inactive.type, String] {
				case (inactive, prefix) =>
					prefix + ": " + inactive.entryName.toLowerCase
				}

		implicit val caseSuspended : Case.Aux[Suspended.type, String, String] =
			at[Suspended.type, String] {
				case (_, prefix) =>
					prefix + ": nothing to see here!"
				}
	}
}

