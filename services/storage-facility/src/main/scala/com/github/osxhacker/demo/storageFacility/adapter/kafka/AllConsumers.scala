package com.github.osxhacker.demo.storageFacility.adapter.kafka

import scala.language.postfixOps

import cats.effect.{
	IO,
	OutcomeIO,
	ResourceIO
	}

import org.typelevel.log4cats.LoggerFactory

import com.github.osxhacker.demo.chassis.adapter.kafka.{
	EventFilter,
	Ingress
	}

import com.github.osxhacker.demo.chassis.domain.event.Region
import com.github.osxhacker.demo.chassis.effect.{
	Pointcut,
	ReadersWriterResource
	}

import com.github.osxhacker.demo.chassis.monitoring.Subsystem
import com.github.osxhacker.demo.storageFacility.adapter.RuntimeSettings
import com.github.osxhacker.demo.storageFacility.adapter.rest.api
import com.github.osxhacker.demo.storageFacility.domain.GlobalEnvironment
import com.github.osxhacker.demo.storageFacility.domain.event.EventChannel
import com.github.osxhacker.demo.storageFacility.domain.scenario.{
	InterpretCompanyEvents,
	InterpretStorageFacilityEvents
	}

import com.github.osxhacker.demo.storageFacility.domain.specification.RegionIs


/**
 * The '''AllConsumers''' type provides all available Kafka consumers,
 * within the context of [[cats.effect.IO]].  Whereas other types similar to
 * '''AllConsumers''' are parameterized on a container ''F'', this type requires
 * the utilities available from and specific to [[cats.effect.IO]].
 */
final case class AllConsumers (private val settings : RuntimeSettings)
	(
		implicit
		private val loggerFactory : LoggerFactory[IO],
		private val pointcut : Pointcut[IO],
		private val environment : ReadersWriterResource[IO, GlobalEnvironment[IO]]
	)
	extends (() => ResourceIO[IO[OutcomeIO[Unit]]])
{
	/// Instance Properties
	implicit private val subsystem = Subsystem ("kafka")


	override def apply () : ResourceIO[IO[OutcomeIO[Unit]]] =
		(company () &> facility ()).background


	private def company () : IO[Unit] =
	{
		val companyEvents = ConsumeCompanyEvents[IO] (
			EventChannel.Company,
			environment,
			settings
			)

		val ingress = Ingress[api.CompanyEvent, AllApiCompanyEventTypes] (
			arrow.CompanyApiEventsToDomain
			)

		val interpreter = InterpretCompanyEvents[IO] ()

		companyEvents (ingress (interpreter ()).mapF (_.value.value.void))
	}


	private def facility () : IO[Unit] =
	{
		val filter = EventFilter[
			IO,
			api.StorageFacilityEvent,
			GlobalEnvironment[IO]
			] ()

		val facilityEvents = ConsumeStorageFacilityEvents[IO] (
			EventChannel.StorageFacility,
			environment,
			settings
			)

		val ingress =
			Ingress[api.StorageFacilityEvent, AllApiStorageFacilityEventTypes] (
				arrow.StorageFacilityApiEventsToDomain
				)

		val interpreter = InterpretStorageFacilityEvents[IO] ()

		Region[IO] (settings.region) flatMap {
			region =>
				val spec = !RegionIs (region) {
					api.StorageFacilityEvent
						.Optics
						.origin
						.andThen (
							api.EventOrigin
								.Optics
								.region
								.asGetter
							)
					}

				facilityEvents (
					(filter (spec) andThen ingress (interpreter ())).mapF (
						_.value.value.void
						)
					)
			}
	}
}

