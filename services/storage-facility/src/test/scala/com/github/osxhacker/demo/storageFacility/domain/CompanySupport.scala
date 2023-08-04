package com.github.osxhacker.demo.storageFacility.domain

import scala.language.{
	implicitConversions,
	postfixOps
	}

import org.scalacheck.{
	Arbitrary,
	Gen
	}

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.chassis.domain.Slug
import com.github.osxhacker.demo.chassis.domain.entity._


/**
 * The '''CompanySupport''' type defines common behaviour for generating
 * [[com.github.osxhacker.demo.storageFacility.domain.Company]] types.
 * By providing `implicit` [[org.scalacheck.Arbitrary]] methods for company
 * types having very specific [[eu.timepit.refined.api.Refined]] types,
 * generating instances has a __much__ higher probability of success.  Without
 * them, it would be virtually impossible to create a valid URN for example.
 */
trait CompanySupport
{
	/// Self Type Constraints
	this : ProjectSpec =>


	/// Instance Properties
	implicit protected val domainCompanyStatus =
		Gen.const[CompanyStatus] (CompanyStatus.Active)

	implicit protected val domainSlug =
		Generators.trimmedString[Slug.Value] (
			1 to 32,
			Gen.alphaLowerChar
			)
			.map (Slug (_))


	/// Implicit Conversions
	implicit def arbDomainCompany (
		implicit
		slug : Gen[Slug],
		status : Gen[CompanyStatus]
		)
		: Arbitrary[Company] =
	{
		val generator = for {
			anId <- Gen.const (Identifier.fromRandom[Company] ())
			slug <- slug
			name <- Generators.trimmedString[Company.Name] (2 to 64)
			status <- status
			} yield Company (
				id = anId,
				slug = slug,
				name = name,
				status = status,
				timestamps = ModificationTimes.now ()
				)

		Arbitrary (generator)
	}
}

