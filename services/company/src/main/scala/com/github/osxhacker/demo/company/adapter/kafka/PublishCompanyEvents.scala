package com.github.osxhacker.demo.company.adapter.kafka

import java.time.{
	OffsetDateTime,
	ZoneOffset
	}

import java.util.UUID.randomUUID

import cats.data.ValidatedNec
import cats.effect._
import fs2.kafka._
import io.circe
import io.scalaland.chimney
import io.scalaland.chimney.TransformerF
import org.typelevel.log4cats.LoggerFactory
import shapeless.{
	syntax => _,
	_
	}

import com.github.osxhacker.demo.chassis.adapter.kafka.{
	AbstractPublishEvents,
	CirceAware
	}

import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.chassis.domain.error.ValidationError
import com.github.osxhacker.demo.chassis.effect.Pointcut
import com.github.osxhacker.demo.company.adapter.RuntimeSettings
import com.github.osxhacker.demo.company.adapter.rest.api
import com.github.osxhacker.demo.company.domain
import com.github.osxhacker.demo.company.domain.event.{
	AllCompanyEvents,
	EventChannel
	}


/**
 * The '''PublishCompanyEvents''' type defines an adapter responsible for
 * emitting [[com.github.osxhacker.demo.company.domain.event]]s using Kafka as
 * the event bus.
 */
final case class PublishCompanyEvents[F[_]] (
	private val settings : RuntimeSettings
	)
	(
		implicit

		/// Needed for `pipe`.
		override protected val async : Async[F],

		/// Needed for `measure`.
		override protected val pointcut : Pointcut[F],

		private val underlyingLoggerFactory : LoggerFactory[F]
	)
	extends AbstractPublishEvents[
		F,
		EventChannel,
		CompanyKeyType,
		CompanyEventType,
		AllCompanyEvents
		] (EventChannel.Company)
		with CirceAware
{
	/// Class Imports
	import PublishCompanyEvents.DomainToApi
	import cats.syntax.all._
	import circe.refined._


	/// Instance Properties
	override protected val numberOfPartitions = settings
		.kafka
		.company
		.numberOfPartitions
		.value

	override protected val producerSettings = ProducerSettings[
		F,
		CompanyKeyType,
		CompanyEventType
		]
		.withBootstrapServers (settings.kafka.servers.value)

	override protected val replicationFactor = settings
		.kafka
		.company
		.replicationFactor
		.value

	override protected val servers = settings
		.kafka
		.servers
		.value

	override protected val topic = settings
		.kafka
		.company
		.topicNameOrDefault (channel)


	override protected def keyFor (event : CompanyEventType) : CompanyKeyType =
		event.id


	override protected def mapToApiEvents (events : fs2.Chunk[AllCompanyEvents])
		: fs2.Chunk[CompanyEventType] =
		events.map {
			event =>
				event.map (DomainToApi)
					.unify
					.valueOr (e => throw e)
			}
}


object PublishCompanyEvents
{
	/// Class Imports
	import chimney.cats._
	import chimney.dsl._
	import domain.transformers._


	/// Class Types
	/**
	 * The '''DomainToApi''' `object` defines a [[shapeless.Poly1]] polymorphic
	 * functor responsible for translating __all__ supported
	 * [[com.github.osxhacker.demo.company.domain.event.CompanyEvent]]s into
	 * `api` representations.  Each `implicit` __must__ result in an
	 * [[com.github.osxhacker.demo.chassis.domain.ErrorOr]] having the common
	 * `api` base type of
	 * [[com.github.osxhacker.demo.company.adapter.rest.api.CompanyEvent]].
	 */
	object DomainToApi
		extends Poly1
	{
		/// Class Types
		type ResultType[A] = Case.Aux[A, ErrorOr[CompanyEventType]]


		/// Implicit Conversions
		implicit val caseCreated : ResultType[domain.event.CompanyCreated] =
			at {
				_.intoF[ValidatedNec[String, +*], api.CompanyCreated]
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
					.leftMap (ValidationError[api.CompanyCreated] (_))
					.toEither
				}

		implicit val caseDeleted : ResultType[domain.event.CompanyDeleted] =
			at {
				_.intoF[ValidatedNec[String, +*], api.CompanyDeleted]
					.withFieldComputedF (_.origin, mkOrigin)
					.transform
					.leftMap (ValidationError[api.CompanyDeleted] (_))
					.toEither
				}

		implicit val caseProfileChanged
			: ResultType[domain.event.CompanyProfileChanged] =
			at {
				_.intoF[ValidatedNec[String, +*], api.CompanyProfileChanged]
					.withFieldComputedF (_.origin, mkOrigin)
					.transform
					.leftMap (ValidationError[api.CompanyProfileChanged] (_))
					.toEither
				}

		implicit val caseSlugChanged
			: ResultType[domain.event.CompanySlugChanged] =
			at {
				_.intoF[ValidatedNec[String, +*], api.CompanySlugChanged]
					.withFieldComputedF (_.origin, mkOrigin)
					.transform
					.leftMap (ValidationError[api.CompanySlugChanged] (_))
					.toEither
				}

		implicit val caseStatusChanged
			: ResultType[domain.event.CompanyStatusChanged] =
			at {
				event =>
					event.status.pfmap[
						DomainStatusToApi.type,
						domain.event.CompanyStatusChanged,
						ErrorOr[api.CompanyStatusChanged]
					] (event)
				}
	}


	/**
	 * The '''DomainStatusToApi''' `object` defines a [[shapeless.Poly2]]
	 * polymorphic functor responsible for translating __all__
	 * [[com.github.osxhacker.demo.company.domain.event.CompanyStatusChanged]]
	 * events into `api` representations.  Each `implicit` __must__ result in an
	 * [[com.github.osxhacker.demo.chassis.domain.ErrorOr]] having the common
	 * `api` base type of
	 * [[com.github.osxhacker.demo.company.adapter.rest.api.CompanyStatusChanged]].
	 */
	object DomainStatusToApi
		extends Poly2
	{
		implicit val caseActivated : Case.Aux[
			domain.CompanyStatus.Active.type,
			domain.event.CompanyStatusChanged,
			ErrorOr[api.CompanyStatusChanged]
			] =
			at {
				case (_, event) =>
					event.intoF[ValidatedNec[String, +*], api.CompanyActivated]
						.withFieldComputedF (_.origin, mkOrigin)
						.transform
						.leftMap (ValidationError[api.CompanyActivated] (_))
						.toEither
				}

		implicit val caseInactivated : Case.Aux[
			domain.CompanyStatus.Inactive.type,
			domain.event.CompanyStatusChanged,
			ErrorOr[api.CompanyStatusChanged]
			] =
			at {
				case (_, event) =>
					event.intoF[ValidatedNec[String, +*], api.CompanyInactivated]
						.withFieldComputedF (_.origin, mkOrigin)
						.transform
						.leftMap (ValidationError[api.CompanyInactivated] (_))
						.toEither
				}

		implicit val caseSuspended : Case.Aux[
			domain.CompanyStatus.Suspended.type,
			domain.event.CompanyStatusChanged,
			ErrorOr[api.CompanyStatusChanged]
			] =
			at {
				case (_, event) =>
					event.intoF[ValidatedNec[String, +*], api.CompanySuspended]
						.withFieldComputedF (_.origin, mkOrigin)
						.transform
						.leftMap (ValidationError[api.CompanySuspended] (_))
						.toEither
			}
	}


	/// Instance Properties
	private val mkOrigin = TransformerF.define[
		ValidatedNec[String, +*],
		domain.event.CompanyEvent,
		api.EventOrigin
		]
		.withFieldConst (_.id, randomUUID ())
		.withFieldConst (_.createdOn, OffsetDateTime.now ())
		.buildTransformer
		.transform (_)
}

