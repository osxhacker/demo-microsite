package com.github.osxhacker.demo.api.company

import java.time.OffsetDateTime

import org.scalacheck.{
	Arbitrary,
	Gen
	}

import com.github.osxhacker.demo.api.ProjectSpec


/**
 * The '''CompanySupport''' type defines common behaviour for interacting with
 * [[com.github.osxhacker.demo.api.company]] types.  By providing `implicit`
 * [[org.scalacheck.Arbitrary]] methods for company types having very specific
 * [[eu.timepit.refined.api.Refined]] types, generating instances has a __much__
 * higher probability of success.  Without them, it would be virtually
 * impossible to create a valid URN for example.
 */
trait CompanySupport
{
	/// Self Type Constraints
	this : ProjectSpec =>


	/// Instance Properties
	implicit protected val companyId = Generators.urn[Company.IdType] (
		"company"
		)

	implicit protected val companySlug =
		Generators.boundedString[Company.SlugType] (
			2 to 32,
			Gen.alphaLowerChar
			)

	implicit protected val companyStatus = Gen.const (CompanyStatus.Active)

	implicit protected lazy val companyVersion =
		Gen.posNum[Int]
			.suchThat (_ < Int.MaxValue)
			.flatMap {
				value =>
					Company.VersionType
						.from (value)
						.fold (_ => Gen.fail, Gen.const)
				}


	/// Implicit Conversions
	protected implicit def arbCompany (
		implicit
		id : Gen[Company.IdType],
		slug : Gen[Company.SlugType],
		status : Gen[CompanyStatus],
		version : Gen[Company.VersionType]
		)
		: Arbitrary[Company] =
	{
		val generator = for {
			anId <- id
			theVersion <- version
			slug <- slug
			status <- status
			name <- Generators.boundedString[Company.NameType] (
				2 to 64,
				Gen.asciiPrintableChar
					.filterNot (_.isWhitespace)
				)

			descriptionLength <- Gen.choose (0, 2048)
			description <- Gen.stringOfN (
				descriptionLength,
				Gen.asciiChar
					.filterNot ("\r\n".indexOf (_) >= 0)
				)
				.map (_.trim)
				.toRefined[Company.DescriptionType] ()
			} yield Company (
				id = anId,
				version = theVersion,
				createdOn = OffsetDateTime.now (),
				lastChanged = OffsetDateTime.now (),
				slug = slug,
				name = name,
				status = status,
				description = description
				)

		Arbitrary (generator)
	}


	protected implicit def arbNewCompany (
		implicit
		slug : Gen[NewCompany.SlugType],
		status : Gen[CompanyStatus]
		)
		: Arbitrary[NewCompany] =
	{
		val generator = for {
			slug <- slug
			status <- status
			name <- Generators.boundedString[NewCompany.NameType] (
				2 to 6,
				Gen.asciiPrintableChar
					.filterNot (_.isWhitespace)
				)

			descriptionLength <- Gen.choose (0, 2048)
			description <- Gen.stringOfN (descriptionLength, Gen.asciiChar)
				.map (_.trim)
				.toRefined[NewCompany.DescriptionType] ()
		} yield NewCompany (
			slug = slug,
			name = name,
			status = status,
			description = description
			)

		Arbitrary (generator)
	}
}
