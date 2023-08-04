package com.github.osxhacker.demo.storageFacility.adapter.kafka

import cats.effect._
import fs2.kafka._
import io.circe

import com.github.osxhacker.demo.chassis.adapter.kafka.{
	AbstractPublishEvents,
	CirceAware
	}

import com.github.osxhacker.demo.chassis.effect.Pointcut
import com.github.osxhacker.demo.storageFacility.adapter.RuntimeSettings
import com.github.osxhacker.demo.storageFacility.domain.event.{
	AllStorageFacilityEvents,
	EventChannel
	}

import arrow.StorageFacilityDomainEventsToApi


/**
 * The '''PublishStorageFacilityEvents''' type defines an adapter responsible
 * for emitting [[com.github.osxhacker.demo.storageFacility.domain.event]]s
 * using Kafka as the event bus.
 */
final case class PublishStorageFacilityEvents[F[_]] (
	private val settings : RuntimeSettings
	)
	(
		implicit

		/// Needed for `pipe`.
		override protected val async : Async[F],

		/// Needed for `measure`.
		override protected val pointcut : Pointcut[F]
	)
	extends AbstractPublishEvents[
		F,
		EventChannel,
		StorageFacilityKeyType,
		StorageFacilityEventType,
		AllStorageFacilityEvents
		] (EventChannel.StorageFacility)
		with CirceAware
{
	/// Class Imports
	import cats.syntax.all._
	import circe.refined._


	/// Instance Properties
	override protected val numberOfPartitions = settings
		.kafka
		.storageFacility
		.numberOfPartitions
		.value

	override protected val producerSettings = ProducerSettings[
		F,
		StorageFacilityKeyType,
		StorageFacilityEventType
		]
		.withBootstrapServers (settings.kafka.servers.value)

	override protected val replicationFactor = settings
		.kafka
		.storageFacility
		.replicationFactor
		.value

	override protected val servers = settings
		.kafka
		.servers
		.value

	override protected val topic = settings
		.kafka
		.storageFacility
		.topicNameOrDefault (channel)


	override protected def keyFor (event : StorageFacilityEventType)
		: StorageFacilityKeyType =
		event.id


	override protected def mapToApiEvents (
		events : fs2.Chunk[AllStorageFacilityEvents]
		)
		: fs2.Chunk[StorageFacilityEventType] =
		events.map (StorageFacilityDomainEventsToApi (_).valueOr (e => throw e))
}

