package com.github.osxhacker.demo.chassis.domain.event

import cats.Applicative
import shapeless.Coproduct


/**
 * The '''NoopEventProducer''' type defines a version of
 * [[com.github.osxhacker.demo.chassis.domain.event.EventProducer]] which
 * unconditionally discards all ''DomainEventsT''.  This is useful for
 * collaborations which require an
 * [[com.github.osxhacker.demo.chassis.domain.event.EventProducer]], but event
 * emission is not desired.  An example of this is when using Domain Object
 * Model Use-Cases during event consumption.
 */
final case class NoopEventProducer[
	F[_],
	ChannelT <: Channel,
	DomainEventsT <: Coproduct
	] (override val channel : ChannelT)
	(implicit private val applicative : Applicative[F])
	extends EventProducer[F, ChannelT, DomainEventsT] (channel)
{
	override def apply[A] (events : EventLog[F, A, DomainEventsT])
		: F[A] =
		events.value


	override def createChannel () : F[Unit] = applicative.unit
}
