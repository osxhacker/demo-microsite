package com.github.osxhacker.demo.storageFacility.adapter.kafka


import cats.effect.{
	Async,
	Temporal
	}

import eu.timepit.refined
import fs2.kafka._
import io.circe

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
 * The '''ConsumeStorageFacilityEvents''' type defines the shell for processing
 * [[com.github.osxhacker.demo.storageFacility.adapter.kafka.StorageFacilityEventType]]s
 * on a per-workflow basis.  There can be multiple instances in the
 * microservice, but each should have only one instance per
 * `channel` / "groupId" pair.
 *
 * Each StorageFacility microservice deployment is configured with what
 * [[com.github.osxhacker.demo.chassis.domain.event.Region]] it belongs to,
 * meaning that this is the determination for using it as the "groupId".
 */
final case class ConsumeStorageFacilityEvents[F[_]] (
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
		override protected val temporal : Temporal[F]
	)
	extends AbstractConsumeEvents[
		F,
		EventChannel,
		GlobalEnvironment[F],
		StorageFacilityKeyType,
		StorageFacilityEventType
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
		ConsumerSettings[F, StorageFacilityKeyType, StorageFacilityEventType]
			.withAutoOffsetReset (AutoOffsetReset.Earliest)
			.withBootstrapServers (settings.kafka.servers.value)
			.withGroupId (settings.region.show)

	override val topic : String =
		settings.kafka
			.company
			.topicNameOrDefault (channel)
}

