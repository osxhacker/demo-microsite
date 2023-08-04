package com.github.osxhacker.demo.company.adapter.kafka.arrow

import io.scalaland.chimney

import com.github.osxhacker.demo.chassis.adapter.kafka.arrow.ApiEventsToDomainLike
import com.github.osxhacker.demo.chassis.domain.ChimneyErrors
import com.github.osxhacker.demo.chassis.domain.entity._
import com.github.osxhacker.demo.chassis.domain.event.{
	Region,
	ServiceFingerprint
	}

import com.github.osxhacker.demo.chassis.monitoring.CorrelationId
import com.github.osxhacker.demo.company.adapter.rest.api
import com.github.osxhacker.demo.company.domain


/**
 * The '''ApiEventsToDomain''' `object` defines the translation from supported
 * [[com.github.osxhacker.demo.company.adapter.rest.api.CompanyEvent]]s to
 * [[com.github.osxhacker.demo.company.domain.event.CompanyEvent]]s.
 */
object ApiEventsToDomain
	extends ApiEventsToDomainLike[api.CompanyEvent]
{
	/// Class Imports
	import chimney.cats._
	import chimney.dsl._
	import shapeless.syntax.inject._
	import domain.transformers._


	/// Instance Properties
	private val mkCorrelationId
		: api.CompanyEvent => ChimneyErrors[CorrelationId] =
		_.origin
			.correlationId
			.intoF[ChimneyErrors, CorrelationId]
			.transform

	private val mkFingerprint
		: api.CompanyEvent => ChimneyErrors[Option[ServiceFingerprint]] =
		_.origin
			.fingerprint
			.intoF[ChimneyErrors, Option[ServiceFingerprint]]
			.transform

	private val mkRegion : api.CompanyEvent => ChimneyErrors[Region] =
		_.origin
			.region
			.intoF[ChimneyErrors, Region]
			.transform


	/// Implicit Conversions
	implicit val caseActivated =
		transform[api.CompanyActivated, domain.event.AllCompanyEvents] {
			_.intoF[ChimneyErrors, domain.event.CompanyStatusChanged]
				.withFieldComputedF (_.correlationId, mkCorrelationId)
				.withFieldComputedF (_.region, mkRegion)
				.withFieldComputedF (_.fingerprint, mkFingerprint)
				.withFieldConst (_.status, domain.CompanyStatus.Active)
				.transform
				.map (_.inject[domain.event.AllCompanyEvents])
			}

	implicit val caseCreated =
		transform[api.CompanyCreated, domain.event.AllCompanyEvents] {
			_.intoF[ChimneyErrors, domain.event.CompanyCreated]
				.withFieldComputedF (_.correlationId, mkCorrelationId)
				.withFieldComputedF (_.region, mkRegion)
				.withFieldComputedF (_.fingerprint, mkFingerprint)
				.withFieldConst (_.version, Version.initial)
				.withFieldComputed (
					_.timestamps,
					ev => ModificationTimes (
						ev.createdOn.toInstant,
						ev.lastChanged.toInstant
						)
					)
				.transform
				.map (_.inject[domain.event.AllCompanyEvents])
			}

	implicit val caseDeleted =
		transform[api.CompanyDeleted, domain.event.AllCompanyEvents] {
			_.intoF[ChimneyErrors, domain.event.CompanyDeleted]
				.withFieldComputedF (_.correlationId, mkCorrelationId)
				.withFieldComputedF (_.region, mkRegion)
				.withFieldComputedF (_.fingerprint, mkFingerprint)
				.transform
				.map (_.inject[domain.event.AllCompanyEvents])
			}

	implicit val caseInactivated =
		transform[api.CompanyInactivated, domain.event.AllCompanyEvents] {
			_.intoF[ChimneyErrors, domain.event.CompanyStatusChanged]
				.withFieldComputedF (_.correlationId, mkCorrelationId)
				.withFieldComputedF (_.region, mkRegion)
				.withFieldComputedF (_.fingerprint, mkFingerprint)
				.withFieldConst (_.status, domain.CompanyStatus.Inactive)
				.transform
				.map (_.inject[domain.event.AllCompanyEvents])
			}

	implicit val caseProfileChanged =
		transform[api.CompanyProfileChanged, domain.event.AllCompanyEvents] {
			_.intoF[ChimneyErrors, domain.event.CompanyProfileChanged]
				.withFieldComputedF (_.correlationId, mkCorrelationId)
				.withFieldComputedF (_.region, mkRegion)
				.withFieldComputedF (_.fingerprint, mkFingerprint)
				.transform
				.map (_.inject[domain.event.AllCompanyEvents])
			}

	implicit val caseSlugChanged =
		transform[api.CompanySlugChanged, domain.event.AllCompanyEvents] {
			_.intoF[ChimneyErrors, domain.event.CompanySlugChanged]
				.withFieldComputedF (_.correlationId, mkCorrelationId)
				.withFieldComputedF (_.region, mkRegion)
				.withFieldComputedF (_.fingerprint, mkFingerprint)
				.transform
				.map (_.inject[domain.event.AllCompanyEvents])
			}

	implicit val caseSuspended =
		transform[api.CompanySuspended, domain.event.AllCompanyEvents] {
			_.intoF[ChimneyErrors, domain.event.CompanyStatusChanged]
				.withFieldComputedF (_.correlationId, mkCorrelationId)
				.withFieldComputedF (_.region, mkRegion)
				.withFieldComputedF (_.fingerprint, mkFingerprint)
				.withFieldConst (_.status, domain.CompanyStatus.Suspended)
				.transform
				.map (_.inject[domain.event.AllCompanyEvents])
			}
}

