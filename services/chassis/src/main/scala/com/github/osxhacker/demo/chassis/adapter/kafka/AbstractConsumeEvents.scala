package com.github.osxhacker.demo.chassis.adapter.kafka

import scala.concurrent.duration._
import scala.language.postfixOps

import cats.Monad
import cats.data.Kleisli
import cats.effect.{
	Async,
	Temporal
	}

import fs2.kafka._
import org.typelevel.log4cats.LoggerFactory

import com.github.osxhacker.demo.chassis
import com.github.osxhacker.demo.chassis.domain.Specification
import com.github.osxhacker.demo.chassis.domain.event.{
	Channel,
	EventConsumer
	}

import com.github.osxhacker.demo.chassis.effect._
import com.github.osxhacker.demo.chassis.monitoring.logging.{
	ContextualLoggerFactory,
	LogInvocation
	}

import com.github.osxhacker.demo.chassis.monitoring.metrics._


/**
 * The '''AbstractConsumeEvents''' type defines common functionality for
 * [[com.github.osxhacker.demo.chassis.domain.event.EventConsumer]]s using
 * [[https://kafka.apache.org/books-and-papers Apache Kafka]].
 */
abstract class AbstractConsumeEvents[
	F[_],
	ChannelT <: Channel,
	EnvT,
	KeyT,
	EventT
	] (
		override val channel : ChannelT,
		protected val guardedEnvironment : ReadersWriterResource[F, EnvT]
	)
	(
		implicit

		/// Needed `KafkaConsumer.stream`.
		protected val async : Async[F],

		/// Needed for `measure`.
		protected val pointcut : Pointcut[F],

		/// Needed for `groupWithin`.
		protected val temporal : Temporal[F],

		/// Needed for '''EventConsumption'''.
		private val underlyingLoggerFactory : LoggerFactory[F]
	)
	extends EventConsumer[F, Channel, EventT, EnvT] (channel)
{
	/// Class Imports
	import AbstractConsumeEvents.EventConsumption
	import cats.syntax.all._
	import chassis.syntax._
	import mouse.boolean._


	/// Instance Properties
	/**
	 * The allow property provides the
	 * [[com.github.osxhacker.demo.chassis.domain.Specification]] used to
	 * determine whether or not the ''EnvT'' allows for event consumption.  The
	 * first time it results in `false`, event consumption will quiesce.
	 */
	protected def allow : Specification[EnvT]

	/**
	 * The consumerSettings property provides a __fully configured__
	 * [[fs2.kafka.ConsumerSettings]] to use.
	 */
	protected def consumerSettings : ConsumerSettings[F, KeyT, EventT]

	/**
	 * The maximumBatchDuration property specifies how long to wait in order to
	 * group [[fs2.kafka.CommittableOffset]]s before sending whatever is
	 * available to Kafka.
	 */
	protected def maximumBatchDuration : FiniteDuration = 5 seconds

	/**
	 * The maximumBatchSize property specifies the maximum number of
	 * [[fs2.kafka.CommittableOffset]]s to send at once to Kafka.
	 */
	protected def maximumBatchSize : Int = 100

	/**
	 * The topic property resolves what Kafka topic to `subscribeTo`.  It is
	 * usually based off of the `channel` or a configuration knob relating to
	 * same.
	 */
	protected def topic : String

	private val advice = EventConsumption[F, ChannelT] (channel)


	final override def apply (interpreter : Kleisli[F, (EventT, EnvT), Unit])
		: F[Unit] =
		KafkaConsumer.stream (consumerSettings)
			.subscribeTo (topic)
			.flatMap (dispatch (interpreter))
			.through {
				_.groupWithin (maximumBatchSize, maximumBatchDuration)
					.evalMap (
						CommittableOffsetBatch.fromFoldableOption (_)
							.commit
						)
				}
			.compile
			.drain


	private def dispatch (interpreter : Kleisli[F, (EventT, EnvT), Unit])
		(consumer : KafkaConsumer[F, KeyT, EventT])
		: fs2.Stream[F, Option[CommittableOffset[F]]] =
		consumer.stream.evalMap {
			committable =>
				guardedEnvironment.reader {
					env =>
						allow (env).fold (
							interpreter (committable.record.value -> env).as (
								committable.offset.pure[Option]
								)
								.onError (_ => consumer.stopConsuming)
								.measure (advice),

							consumer.stopConsuming
								.as (none[CommittableOffset[F]])
							)
					}
			}
}


object AbstractConsumeEvents
{
	/// Class Types
	/**
	 * The '''EventConsumption''' type defines the
	 * [[com.github.osxhacker.demo.chassis.monitoring.metrics.MetricsAdvice]]
	 * specific to
	 * [[https://kafka.apache.org/books-and-papers Apache Kafka]]-based
	 * '''AbstractConsumeEvents'''.
	 */
	final private case class EventConsumption[F[_], ChannelT <: Channel] (
		private val channel : ChannelT
		)
		(
			implicit

			override protected val monad : Monad[F],

			/// Needed for '''ContextualLoggerFactory'''.
			private val underlyingLoggerFactory : LoggerFactory[F]
		)
		extends DefaultAdvice[F, Option[CommittableOffset[F]]] ()
			with MetricsAdvice[F, Option[CommittableOffset[F]]]
			with InvocationCounters[F, Option[CommittableOffset[F]]]
			with LogInvocation[F, Option[CommittableOffset[F]]]
			with ScopeConsumerOperations[F, Option[CommittableOffset[F]]]
	{
		/// Instance Properties
		override val component = "kafka"
		override val group = subgroup (
			super.group,
			"event",
			"consume",
			channel.entryName
			)

		override val operation =
			s"CONSUME ${channel.getClass.getSimpleName.stripSuffix ("$")}"

		override protected val loggerFactory =
			ContextualLoggerFactory[F](underlyingLoggerFactory) {
				Map (
					"channel" -> channel.entryName,
					"operation" -> operation
					)
				}
	}
}

