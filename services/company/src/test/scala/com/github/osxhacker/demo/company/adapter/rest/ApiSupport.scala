package com.github.osxhacker.demo.company.adapter.rest

import java.time.OffsetDateTime

import org.scalacheck.{
	Arbitrary,
	Gen
	}

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.company.domain


/**
 * The '''ApiSupport''' type defines common behaviour for generating
 * [[com.github.osxhacker.demo.company.adapter.rest.api]] types.
 * By providing `implicit` [[org.scalacheck.Arbitrary]] methods for API types,
 * most having very specific [[eu.timepit.refined.api.Refined]] types,
 * generating instances has a __much__ higher probability of success.  Without
 * them, it would be virtually impossible to create a valid URN for example.
 */
trait ApiSupport
{
	/// Self Type Constraints
	this : ProjectSpec =>


	/// Instance Properties
	implicit protected lazy val apiCompanyId =
		Generators.urn[api.Company.IdType] (
			domain.Company
				.companyNamespace ()
				.value
			)

	implicit protected lazy val apiCompanyStatus =
		Gen.const (api.CompanyStatus.Active)

	implicit protected lazy val apiCompanyVersion =
		Gen.posNum[Int]
			.suchThat (_ < Int.MaxValue)
			.flatMap {
				value =>
					api.Company
						.VersionType
						.from (value)
						.fold (_ => Gen.fail, Gen.const)
				}

	/// Implicit Conversions
	implicit protected def arbApiNewCompany (
		implicit status : Gen[api.CompanyStatus]
		)
		: Arbitrary[api.NewCompany] = {
		val generator = for {
			slug <- Generators.boundedString[api.Company.SlugType] (
				4 to 10,
				Gen.alphaLowerChar
				)

			name <- Generators.boundedString[api.NewCompany.NameType] (
				2 to 64
				)

			theStatus <- status

			description <- Generators.boundedString[api.NewCompany.DescriptionType] (
				8 to 128
				)
			} yield api.NewCompany (
				slug = slug,
				name = name,
				status = theStatus,
				description = description
				)

		Arbitrary (generator)
	}


	implicit protected def arbApiCompany (
		implicit
		status : Gen[api.CompanyStatus],
		companyId : Gen[api.Company.IdType],
		version : Gen[api.Company.VersionType]
		)
		: Arbitrary[api.Company] =
	{
		val generator = for {
			anId <- companyId
			theVersion <- version
			slug <- Generators.boundedString[api.Company.SlugType] (
				4 to 10,
				Gen.alphaLowerChar
				)

			name <- Generators.trimmedString[api.Company.NameType] (
				2 to 64
				)

			theStatus <- status
			description <- Generators.trimmedString[api.NewCompany.DescriptionType] (
				8 to 128
				)
		} yield api.Company (
			id = anId,
			version = theVersion,
			slug = slug,
			createdOn = OffsetDateTime.now (),
			lastChanged = OffsetDateTime.now (),
			name = name,
			status = theStatus,
			description = description
			)

		Arbitrary (generator)
	}
}

