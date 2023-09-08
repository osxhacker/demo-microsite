package com.github.osxhacker.demo.chassis.adapter.kafka

import cats.{
	ApplicativeThrow,
	FlatMap
	}

import cats.effect.Async
import fs2.kafka._
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.errors.TopicExistsException
import org.typelevel.log4cats.LoggerFactory
import shapeless.{
	syntax => _,
	_
	}

import com.github.osxhacker.demo.chassis
import com.github.osxhacker.demo.chassis.domain.event.{
	Channel,
	EventLog,
	EventProducer
	}

import com.github.osxhacker.demo.chassis.effect._
import com.github.osxhacker.demo.chassis.monitoring.logging.{
	ContextualLoggerFactory,
	LogInvocation
	}

import com.github.osxhacker.demo.chassis.monitoring.metrics.{
	InvocationCounters,
	MetricsAdvice,
	ScopeProducerOperations
	}


/**
 * The '''AbstractPublishEvents''' type defines common functionality for
 * [[com.github.osxhacker.demo.chassis.domain.event.EventProducer]]s using
 * [[https://kafka.apache.org/books-and-papers Apache Kafka]].
 */
abstract class AbstractPublishEvents[
	F[_],
	ChannelT <: Channel,
	KeyT,
	EventT,
	DomainEventsT <: Coproduct
	] (override val channel : ChannelT)
	(
		implicit

		/// Needed for `pipe`.
		protected val async : Async[F],

		/// Needed for `measure`.
		protected val pointcut : Pointcut[F],

		/// Needed for '''EventPublishing'''.
		private val underlyingLoggerFactory : LoggerFactory[F]
	)
	extends EventProducer[F, ChannelT, DomainEventsT] (channel)
{
	/// Class Imports
	import AbstractPublishEvents.EventPublishing
	import cats.syntax.all._
	import chassis.syntax._


	/// Instance Properties
	/**
	 * The numberOfPartitions property specifies how many partitions for the
	 * `topic` Kafka will create __iff__ the `topic` is created.
	 */
	protected def numberOfPartitions : Int

	/**
	 * The producerSettings property provides a __fully configured__
	 * [[fs2.kafka.ProducerSettings]] to use.
	 */
	protected def producerSettings : ProducerSettings[F, KeyT, EventT]

	/**
	 * The replicationFactor property controls how many servers will replicate
	 * each message that is written. If you have a replication factor of 3 then
	 * up to 2 servers can fail before you will lose access to your data.
	 */
	protected def replicationFactor : Short

	/**
	 * What Kafka servers to initially connect to.  The string must be a
	 * comma-separated list of "hostname:port" with no spaces.
	 */
	protected def servers : String

	/**
	 * The topic property resolves what Kafka topic to publish events.  It is
	 * usually based off of the `channel` or a configuration knob relating to
	 * same.
	 */
	protected def topic : String


	/**
	 * The keyFor method is expected to produce a ''KeyT'' from the given
	 * '''event'''.
	 */
	protected def keyFor (event : EventT) : KeyT


	/**
	 * The mapToApiEvents method is expected to create a [[fs2.Chunk]] from
	 * zero or more ''DomainEventsT'' in the given '''events''' instance.
	 */
	protected def mapToApiEvents (events : fs2.Chunk[DomainEventsT])
		: fs2.Chunk[EventT]


	final override def apply[A] (events : EventLog[F, A, DomainEventsT])
		: F[A] =
		events.run.flatMap {
			/// Do not go through the overhead of connecting to Kafka if there
			/// are no `events` to send.
			case (events, a) if events.isEmpty =>
				a.pure[F]

			case (events, a) =>
				KafkaProducer.pipe (producerSettings)
					.apply (
						fs2.Stream
							.emits (events.toVector)
							.mapChunks (toRecords)
							.chunks
						)
					.unchunks
					.compile
					.lastOrError
					.as (a)
					.measure (EventPublishing[F, ChannelT, A] (channel))
			}


	final override def createChannel () : F[Unit] =
		KafkaAdminClient.resource[F] (AdminClientSettings (servers))
			.use {
				client =>
					/// Attempt to create the channel events topic, allowing an
					/// "it already exists" exception to represent success.
					client.createTopic (
						new NewTopic (
							topic,
							numberOfPartitions,
							replicationFactor
							)
						)
						.recover {
							case _ : TopicExistsException =>
								()
							}
				}


	private def toRecords (chunk : fs2.Chunk[DomainEventsT])
		: fs2.Chunk[ProducerRecord[KeyT, EventT]] =
		mapToApiEvents (chunk).map {
			mapped =>
				ProducerRecord (topic, keyFor (mapped), mapped)
			}
}


object AbstractPublishEvents
{
	/// Class Types
	/**
	 * The '''EventPublishing''' type defines the
	 * [[com.github.osxhacker.demo.chassis.monitoring.metrics.MetricsAdvice]]
	 * specific to
	 * [[https://kafka.apache.org/books-and-papers Apache Kafka]]-based
	 * '''AbstractPublishEvents'''.
	 */
	final private case class EventPublishing[F[_], ChannelT <: Channel, ResultT] (
		private val channel : ChannelT
		)
		(
			implicit

			/// Needed for '''Advice'''.
			override protected val applicativeThrow : ApplicativeThrow[F],

			/// Needed for '''LogInvocation'''.
			override protected val flatMap : FlatMap[F],


			/// Needed for '''ContextualLoggerFactory'''.
			private val underlyingLoggerFactory : LoggerFactory[F]
		)
		extends DefaultAdvice[F, ResultT] ()
			with MetricsAdvice[F, ResultT]
			with InvocationCounters[F, ResultT]
			with LogInvocation[F, ResultT]
			with ScopeProducerOperations[F, ResultT]
	{
		/// Instance Properties
		override val component = "kafka"
		override val group = subgroup (
			super.group,
			"event",
			"publish",
			channel.entryName
			)

		override val operation =
			s"PRODUCE ${channel.getClass.getSimpleName.stripSuffix ("$")}"

		override protected val loggerFactory =
			ContextualLoggerFactory[F] (underlyingLoggerFactory) {
				Map (
					"channel" -> channel.entryName,
					"operation" -> operation
					)
				} (applicativeThrow)
	}
}

