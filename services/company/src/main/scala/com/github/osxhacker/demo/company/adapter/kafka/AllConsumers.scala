package com.github.osxhacker.demo.company.adapter.kafka

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

import com.github.osxhacker.demo.chassis.domain.event.ServiceFingerprint
import com.github.osxhacker.demo.chassis.effect.{
	Pointcut,
	ReadersWriterResource
	}

import com.github.osxhacker.demo.chassis.monitoring.Subsystem
import com.github.osxhacker.demo.company.adapter.RuntimeSettings
import com.github.osxhacker.demo.company.adapter.kafka.ConsumeCompanyEvents
import com.github.osxhacker.demo.company.adapter.kafka.specification.DifferentService
import com.github.osxhacker.demo.company.adapter.rest.api
import com.github.osxhacker.demo.company.domain.GlobalEnvironment
import com.github.osxhacker.demo.company.domain.event.EventChannel
import com.github.osxhacker.demo.company.domain.scenario.InterpretCompanyEvents


/**
 * The '''AllConsumers''' type provides all available Kafka consumers,
 * within the context of [[cats.effect.IO]].  Whereas other types similar to
 * '''AllConsumers''' are parameterized on a container ''F'', this type requires
 * the utilities available and specific to [[cats.effect.IO]].
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
	/// Class Imports
	import cats.syntax.applicative._


	/// Instance Properties
	implicit private val subsystem = Subsystem ("kafka")


	override def apply () : ResourceIO[IO[OutcomeIO[Unit]]] =
	{
		val filter = EventFilter[IO, api.CompanyEvent, GlobalEnvironment[IO]] ()
		val all = for {
			fingerprint <- environment.reader (_.fingerprint.pure[IO])
			unit <- company (filter, fingerprint)
			} yield unit

		all.background
	}


	private def company (
		filter : EventFilter[IO, api.CompanyEvent, GlobalEnvironment[IO]],
		fingerprint : ServiceFingerprint
		)
		: IO[Unit] =
	{
		val companyEvents = ConsumeCompanyEvents[IO] (
			EventChannel.Company,
			fingerprint,
			environment,
			settings
			)

		val ingress = Ingress[api.CompanyEvent, AllApiCompanyEventTypes] (
			arrow.ApiEventsToDomain
			)

		val interpreter = InterpretCompanyEvents[IO] ()

		companyEvents (
			(
				filter (DifferentService (fingerprint)) andThen
				ingress (interpreter ())
			)
				.mapF (_.value.value.void)
			)
	}
}

