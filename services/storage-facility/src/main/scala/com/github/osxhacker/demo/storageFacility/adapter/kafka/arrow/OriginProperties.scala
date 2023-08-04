package com.github.osxhacker.demo.storageFacility.adapter.kafka.arrow

import io.scalaland.chimney

import com.github.osxhacker.demo.chassis.adapter.kafka.arrow.ApiEventsToDomainLike
import com.github.osxhacker.demo.chassis.domain.ChimneyErrors
import com.github.osxhacker.demo.chassis.domain.event.{
	Region,
	ServiceFingerprint
	}

import com.github.osxhacker.demo.chassis.monitoring.CorrelationId
import com.github.osxhacker.demo.storageFacility.adapter.rest.api
import com.github.osxhacker.demo.storageFacility.domain


/**
 * The '''OriginProperties''' type defines transformation support for
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.api.DomainEvent]]
 * properties shared across all ''ApiBaseT'' types.
 */
private[arrow] trait OriginProperties[ApiBaseT <: api.DomainEvent]
{
	/// Self Type Constraints
	this : ApiEventsToDomainLike[ApiBaseT] =>


	/// Class Imports
	import chimney.cats._
	import chimney.dsl._
	import domain.transformers._


	/// Instance Properties
	protected val mkCorrelationId
		: ApiBaseT => ChimneyErrors[CorrelationId] =
		_.origin
			.correlationId
			.intoF[ChimneyErrors, CorrelationId]
			.transform

	protected val mkFingerprint
		: ApiBaseT => ChimneyErrors[Option[ServiceFingerprint]] =
		_.origin
			.fingerprint
			.intoF[ChimneyErrors, Option[ServiceFingerprint]]
			.transform

	protected val mkRegion : ApiBaseT => ChimneyErrors[Region] =
		_.origin
			.region
			.intoF[ChimneyErrors, Region]
			.transform
}

