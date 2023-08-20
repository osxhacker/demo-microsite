package com.github.osxhacker.demo.storageFacility.adapter.kafka

import cats.effect.{
	Async,
	Temporal
	}

import eu.timepit.refined
import fs2.kafka._
import io.circe
import org.typelevel.log4cats.LoggerFactory

import com.github.osxhacker.demo.chassis.adapter.kafka.{
	AbstractConsumeEvents,
	CirceAware
	}

import com.github.osxhacker.demo.chassis.domain.Specification
import com.github.osxhacker.demo.chassis.effect.{
	Pointcut,
	ReadersWriterResource
	}

import com.github.osxhacker.demo.storageFacility.adapter.RuntimeSettings
import com.github.osxhacker.demo.storageFacility.domain.GlobalEnvironment
import com.github.osxhacker.demo.storageFacility.domain.event.EventChannel


/**
 * The '''ConsumeCompanyEvents''' type defines the shell for processing
 * [[com.github.osxhacker.demo.storageFacility.adapter.kafka.CompanyEventType]]s
 * on a per-workflow basis.  There can be multiple instances in the
 * microservice, but each should have only one instance per
 * `channel` / "groupId" pair.
 *
 * The Company services are the originators of
 * [[com.github.osxhacker.demo.storageFacility.adapter.kafka.CompanyEventType]]s.
 * As such, the StorageFacility services consume them on a per
 * [[com.github.osxhacker.demo.chassis.domain.event.Region]] basis.
 */
final case class ConsumeCompanyEvents[F[_]] (
	override val channel : EventChannel,
	override protected val guardedEnvironment : ReadersWriterResource[
		F, GlobalEnvironment[F]
		],

	private val settings : RuntimeSettings
	)
	(
		implicit

		override protected val async : Async[F],
		override protected val pointcut : Pointcut[F],
		private val underlyingLoggerFactory : LoggerFactory[F],
		override protected val temporal : Temporal[F]
	)
	extends AbstractConsumeEvents[
		F,
		EventChannel,
		GlobalEnvironment[F],
		CompanyKeyType,
		CompanyEventType
		] (channel, guardedEnvironment)
		with CirceAware
{
	/// Class Imports
	import cats.syntax.show._
	import circe.refined._
	import refined.cats._


	/// Instance Properties
	override val allow : Specification[GlobalEnvironment[F]] = _.isOnline
	override val consumerSettings =
		ConsumerSettings[F, CompanyKeyType, CompanyEventType]
			.withAutoOffsetReset (AutoOffsetReset.Earliest)
			.withBootstrapServers (settings.kafka.servers.value)
			.withGroupId (settings.region.show)

	override val topic : String =
		settings.kafka
			.company
			.topicNameOrDefault (channel)
}

