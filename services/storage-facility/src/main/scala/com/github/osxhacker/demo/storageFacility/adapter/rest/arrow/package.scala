package com.github.osxhacker.demo.storageFacility.adapter.rest

import java.time.ZoneOffset

import io.scalaland.chimney
import io.scalaland.chimney.TransformerF

import com.github.osxhacker.demo.chassis.domain.{
	ChimneyErrors,
	NaturalTransformations
	}

import com.github.osxhacker.demo.storageFacility.domain


/**
 * The '''arrow''' `package` defines types responsible for transforming between
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.api]] and
 * [[com.github.osxhacker.demo.storageFacility.domain]] types for
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest]] controllers.
 */
package object arrow
	extends NaturalTransformations
{
	/// Class Imports
	import chimney.cats._
	import domain.transformers._


	/// Implicit Conversions
	implicit val storageFacilityToApiChimneyTransformer =
		TransformerF.define[
			ChimneyErrors,
			domain.StorageFacility,
			api.StorageFacility
			]
			.withFieldComputed (
				_.createdOn,
				_.timestamps.createdOn.atOffset (ZoneOffset.UTC)
				)
			.withFieldComputed (
				_.lastChanged,
				_.timestamps.lastChanged.atOffset (ZoneOffset.UTC)
				)
			.buildTransformer

	implicit val companyToStorageFacilityViewChimneyTransformer =
		TransformerF.define[
			ChimneyErrors,
			domain.Company,
			api.StorageFacilityCompanyView
			]
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

