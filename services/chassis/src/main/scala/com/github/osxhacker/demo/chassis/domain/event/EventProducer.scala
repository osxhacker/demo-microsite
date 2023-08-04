package com.github.osxhacker.demo.chassis.domain.event

import cats.MonadThrow
import org.typelevel.log4cats.LoggerFactory
import shapeless.Coproduct


/**
 * The '''EventProducer''' type defines the Domain Object Model ability to
 * produce an arbitrary number of events, each of which __must__ be a member of
 * ''DomainEventsT''.  Implementations are expected to be able to send multiple
 * domain events, including having the ability to map from the Domain Object
 * Model ''DomainEventsT'' types to suitable anti-corruption layer
 * representations.
 *
 * Any errors encountered during transmission will be represented in ''F''.
 */
abstract class EventProducer[
	F[_],
	ChannelT <: Channel,
	DomainEventsT <: Coproduct
	] (val channel : ChannelT)
{
	/**
	 * The apply method initiates emission of the given '''events'''.  Any
	 * errors encountered during transmission will be represented in ''F''.
	 */
	def apply[A] (events : EventLog[F, A, DomainEventsT]) : F[A]


	/**
	 * The createChannel method attempts to initialize the event subsystem such
	 * that the `channel` exists and is available for use by __any__ process.
	 * Implementations __must__ be idempotent and support multiple invocations.
	 */
	def createChannel () : F[Unit]
}


object EventProducer
{
	/// Class Imports
	import cats.syntax.all._


	/**
	 * The createChannelFor method attempts to ensure the
	 * [[com.github.osxhacker.demo.chassis.domain.event.Channel]] for a given
	 * '''producer''' exists and is ready for use.
	 */
	def createChannelFor[F[_], ChannelT <: Channel, DomainEventsT <: Coproduct] (
		producer : EventProducer[F, ChannelT, DomainEventsT]
		)
		(
			implicit
			monadThrow : MonadThrow[F],
			loggerFactory : LoggerFactory[F]
		)
		: F[EventProducer[F, ChannelT, DomainEventsT]] =
		producer.createChannel ()
			.as (producer)
			.onError {
				problem =>
					loggerFactory.create
						.flatMap (
							_.error (problem) (
								s"unable to create event channel for: ${producer.channel}"
								)
							)
				}
}

