package com.github.osxhacker.demo.storageFacility.adapter.kafka.arrow

import java.time.{
	OffsetDateTime,
	ZoneOffset
	}

import java.util.UUID.randomUUID

import io.scalaland.chimney
import io.scalaland.chimney.TransformerF
import shapeless.{
	syntax => _,
	_
	}

import com.github.osxhacker.demo.chassis.domain.{
	ChimneyErrors,
	ErrorOr
	}

import com.github.osxhacker.demo.chassis.domain.error.{
	LogicError,
	ValidationError
	}

import com.github.osxhacker.demo.storageFacility.adapter.kafka.StorageFacilityEventType
import com.github.osxhacker.demo.storageFacility.adapter.rest.api
import com.github.osxhacker.demo.storageFacility.domain
import com.github.osxhacker.demo.storageFacility.domain.event.AllStorageFacilityEvents


/**
 * The '''StorageFacilityDomainEventsToApi''' `object` defines the translation
 * from supported
 * [[com.github.osxhacker.demo.storageFacility.domain.event.StorageFacilityEvent]]s
 * to
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.api.StorageFacilityEvent]]s.
 * It is the complement of the
 * [[com.github.osxhacker.demo.storageFacility.adapter.kafka.arrow.StorageFacilityApiEventsToDomain]]
 * [[https://typelevel.org/cats/typeclasses/arrow.html Arrow]].
 */
object StorageFacilityDomainEventsToApi
{
	/// Class Imports
	import chimney.cats._
	import chimney.dsl._
	import domain.transformers._


	/// Class Types
	/**
	 * The '''DomainStatusToApi''' `object` defines a [[shapeless.Poly2]]
	 * polymorphic functor responsible for translating __all__
	 * [[com.github.osxhacker.demo.storageFacility.domain.event.StorageFacilityStatusChanged]]
	 * events into `api` representations.  Each `implicit` __must__ result in an
	 * [[com.github.osxhacker.demo.chassis.domain.ErrorOr]] having the common
	 * `api` base type of
	 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.api.StorageFacilityStatusChanged]].
	 */
	object DomainStatusToApi
		extends Poly2
	{
		/// Class Types
		type ResultType[A] = Case.Aux[
			A,
			domain.event.StorageFacilityStatusChanged,
			ErrorOr[api.StorageFacilityStatusChanged]
			]


		/// Implicit Conversions
		implicit val caseActivated
			: ResultType[domain.StorageFacilityStatus.Active.type] =
			at {
				(_, event) =>
					event
						.intoF[ChimneyErrors, api.StorageFacilityActivated]
						.withFieldComputedF (_.origin, mkOrigin)
						.transform
						.leftMap (
							ValidationError[api.StorageFacilityActivated] (_)
							)
						.toEither
				}

		implicit val caseClosed
			: ResultType[domain.StorageFacilityStatus.Closed.type] =
			at {
				(_, event) =>
					event
						.intoF[ChimneyErrors, api.StorageFacilityClosed]
						.withFieldComputedF (_.origin, mkOrigin)
						.transform
						.leftMap (
							ValidationError[api.StorageFacilityClosed] (_)
							)
						.toEither
				}

		implicit val caseUnderConstruction
			: ResultType[domain.StorageFacilityStatus.UnderConstruction.type] =
			at {
				(_, _) =>
					Left (LogicError ("invalid status transition"))
				}
	}


	/**
	 * The '''MapEvent''' `object` defines a [[shapeless.Poly1]] polymorphic
	 * functor responsible for translating __all__ supported
	 * [[com.github.osxhacker.demo.storageFacility.domain.event.StorageFacilityEvent]]s
	 * into `api` representations.  Each `implicit` __must__ result in an
	 * [[com.github.osxhacker.demo.chassis.domain.ErrorOr]] having the common
	 * `api` base type of
	 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.api.StorageFacilityEvent]].
	 */
	object MapEvent
		extends Poly1
	{
		/// Class Types
		type ResultType[A] = Case.Aux[A, ErrorOr[StorageFacilityEventType]]


		/// Implicit Conversions
		implicit val caseCreated
			: ResultType[domain.event.StorageFacilityCreated] =
			at {
				_.intoF[ChimneyErrors, api.StorageFacilityCreated]
					.withFieldComputedF (_.origin, mkOrigin)
					.withFieldComputed (
						_.createdOn,
						_.timestamps.createdOn.atOffset (ZoneOffset.UTC)
						)
					.withFieldComputed (
						_.lastChanged,
						_.timestamps.lastChanged.atOffset (ZoneOffset.UTC)
						)
					.transform
					.leftMap (ValidationError[api.StorageFacilityCreated] (_))
					.toEither
				}

		implicit val caseDeleted
			: ResultType[domain.event.StorageFacilityDeleted] =
			at {
				_.intoF[ChimneyErrors, api.StorageFacilityDeleted]
					.withFieldComputedF (_.origin, mkOrigin)
					.transform
					.leftMap (ValidationError[api.StorageFacilityDeleted] (_))
					.toEither
				}

		implicit val caseProfileChanged
			: ResultType[domain.event.StorageFacilityProfileChanged] =
			at {
				_.intoF[ChimneyErrors, api.StorageFacilityProfileChanged]
					.withFieldComputedF (_.origin, mkOrigin)
					.transform
					.leftMap (
						ValidationError[api.StorageFacilityProfileChanged] (_)
						)
					.toEither
				}

		implicit val caseStatusChanged
			: ResultType[domain.event.StorageFacilityStatusChanged] =
			at {
				event =>
					event.status
						.pfmap[
							DomainStatusToApi.type,
							domain.event.StorageFacilityStatusChanged,
							ErrorOr[api.StorageFacilityStatusChanged]
							] (event)
				}
	}


	/// Instance Properties
	private val mkOrigin = TransformerF.define[
		ChimneyErrors,
		domain.event.StorageFacilityEvent,
		api.EventOrigin
		]
		.withFieldConst (_.id, randomUUID ())
		.withFieldConst (_.createdOn, OffsetDateTime.now ())
		.buildTransformer
		.transform (_)


	def apply (event : AllStorageFacilityEvents)
		: ErrorOr[StorageFacilityEventType] =
		event.map (MapEvent)
			.unify
}

