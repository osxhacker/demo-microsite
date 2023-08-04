package com.github.osxhacker.demo.company.domain

import scala.language.{
	implicitConversions,
	postfixOps
	}

import org.scalacheck.{
	Arbitrary,
	Gen
	}

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.chassis.domain.{
	ErrorOr,
	Slug
	}

import com.github.osxhacker.demo.chassis.domain.entity._


/**
 * The '''CompanySupport''' type defines common behaviour for
 * generating
 * [[com.github.osxhacker.demo.company.domain.Company]] types.
 * By providing `implicit` [[org.scalacheck.Arbitrary]] methods for
 * '''Company''' types having very specific
 * [[eu.timepit.refined.api.Refined]] types, generating instances has a
 * __much__ higher probability of success.  Without them, it would be virtually
 * impossible to create a valid URN for example.
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

	implicit protected val domainVersion =
		Gen.posNum[Int]
			.suchThat (_ < Int.MaxValue)
			.flatMap {
				value =>
					Version[ErrorOr] (value).fold (_ => Gen.fail, Gen.const)
				}


	/// Implicit Conversions
	implicit protected def arbDomainCompany (
		implicit
		slug : Gen[Slug],
		status : Gen[CompanyStatus],
		version : Gen[Version]
		)
		: Arbitrary[Company] =
	{
		val generator = for {
			anId <- Gen.const (Identifier.fromRandom[Company] ())
			theVersion <- version
			slug <- slug
			status <- status
			name <- Generators.trimmedString[Company.Name] (2 to 64)
			descriptionLength <- Gen.choose (0, 2048)
			description <- Gen.stringOfN (
				descriptionLength,
				Gen.asciiChar
					.filterNot ("\r\n".indexOf (_) >= 0)
				)
				.map (_.trim)
				.toRefined[Company.Description] ()
			} yield Company (
				id = anId,
				version = theVersion,
				slug = slug,
				name = name,
				status = status,
				description = description,
				timestamps = ModificationTimes.now ()
				)

		Arbitrary (generator)
	}
}

