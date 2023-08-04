package com.github.osxhacker.demo.company.adapter.rest

import java.time.ZoneOffset

import cats.data.ValidatedNec
import io.scalaland.chimney.TransformerF

import com.github.osxhacker.demo.chassis.domain.NaturalTransformations
import com.github.osxhacker.demo.chassis.domain.entity.{
	Identifier,
	ModificationTimes,
	Version
	}

import com.github.osxhacker.demo.company.domain


/**
 * The '''arrow''' `package` defines types responsible for transforming between
 * [[com.github.osxhacker.demo.company.adapter.rest.api]] and
 * [[com.github.osxhacker.demo.company.domain]] types for
 * [[com.github.osxhacker.demo.company.adapter.rest]] controllers.
 */
package object arrow
	extends NaturalTransformations
{
	/// Class Imports
	import domain.transformers._
	import io.scalaland.chimney.cats._


	/// Implicit Conversions
	implicit val newCompanyFromApiChimneyTransformer =
		TransformerF
			.define[ValidatedNec[String, +*], api.NewCompany, domain.Company]
			.withFieldComputed (
				_.id,
				_ => Identifier.fromRandom[domain.Company] ()
				)
			.withFieldConst (_.version, Version.initial)
			.withFieldComputed (
				_.timestamps,
				_ => ModificationTimes.now ()
				)
			.buildTransformer

	implicit val companyFromApiChimneyTransformer =
		TransformerF
			.define[ValidatedNec[String, +*], api.Company, domain.Company]
			.withFieldComputed (
				_.timestamps,
				rec =>
					ModificationTimes (
						rec.createdOn.toInstant,
						rec.lastChanged.toInstant
						)
				)
			.buildTransformer


	implicit val companyToApiChimneyTransformer =
		TransformerF
			.define[ValidatedNec[String, +*], domain.Company, api.Company]
			.withFieldComputed (
				_.createdOn,
				_.timestamps.createdOn.atOffset (ZoneOffset.UTC)
				)
			.withFieldComputed (
				_.lastChanged,
				_.timestamps.lastChanged.atOffset (ZoneOffset.UTC)
				)
			.buildTransformer
}

