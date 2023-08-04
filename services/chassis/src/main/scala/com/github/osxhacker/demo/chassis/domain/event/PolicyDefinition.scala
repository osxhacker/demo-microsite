package com.github.osxhacker.demo.chassis.domain.event

import scala.language.implicitConversions

import cats.Applicative
import shapeless.Coproduct


/**
 * The '''PolicyDefinition''' type serves as the common ancestor for
 * [[com.github.osxhacker.demo.chassis.domain.event.EventPolicy]] generation.
 *
 * @see [[com.github.osxhacker.demo.chassis.domain.event.EventPolicy]]
 */
sealed trait PolicyDefinition[EnvT]
{
	/**
	 * The emitEventsFor method creates a
	 * [[com.github.osxhacker.demo.chassis.domain.event.EventPolicy]] which will
	 * allow event publishing.
	 */
	protected def emitEventsFor[
		F[_],
		ChannelT <: Channel,
		DomainEventsT <: Coproduct
		] (producer : EnvT => EventProducer[F, ChannelT, DomainEventsT])
		: EventPolicy[F, EnvT, DomainEventsT] =
		new EventPolicy[F, EnvT, DomainEventsT] {
			override type ChannelType = ChannelT


			override def apply (env : EnvT)
				: EventProducer[F, ChannelType, DomainEventsT] =
				producer (env)
			}


	/**
	 * The suppressEventsFor method creates a
	 * [[com.github.osxhacker.demo.chassis.domain.event.EventPolicy]] which will
	 * deny event publishing.
	 */
	protected def suppressEventsFor[
		F[_],
		ChannelT <: Channel,
		DomainEventsT <: Coproduct
		] (producer : EnvT => EventProducer[F, ChannelT, DomainEventsT])
		(implicit applicative : Applicative[F])
		: EventPolicy[F, EnvT, DomainEventsT] =
		new EventPolicy[F, EnvT, DomainEventsT] {
			override type ChannelType = ChannelT


			override def apply (env : EnvT)
				: EventProducer[F, ChannelType, DomainEventsT] =
				NoopEventProducer[F, ChannelType, DomainEventsT] (
					producer (env).channel
					)
		}
}


/**
 * The '''EmitEvents''' type defines the
 * [[com.github.osxhacker.demo.chassis.domain.event.EventPolicy]] which allows
 * ''DomainEventsT'' publishing by way of the resolution of the
 * [[com.github.osxhacker.demo.chassis.domain.event.EventProducer]] associated
 * with the ''DomainEventsT'' within ''EnvT''.
 *
 * Note that the `implicit` method has the same name as found in
 * [[com.github.osxhacker.demo.chassis.domain.event.SuppressEvents]] so that
 * a compilation error is forced should both be in the inheritance heirarchy.
 */
trait EmitEvents[EnvT, DomainEventsT <: Coproduct]
	extends PolicyDefinition[EnvT]
{
	/// Implicit Conversions
	final implicit def defaultEventsPolicy[F[_], ChannelT <: Channel] (
		implicit producer : EnvT => EventProducer[F, ChannelT, DomainEventsT]
		)
		: EventPolicy[F, EnvT, DomainEventsT] =
		emitEventsFor (producer)
}


/**
 * The '''SuppressEvents''' type defines the
 * [[com.github.osxhacker.demo.chassis.domain.event.EventPolicy]] which denies
 * ''DomainEventsT'' publishing which would otherwise be done with the
 * [[com.github.osxhacker.demo.chassis.domain.event.EventProducer]] associated
 * with the ''DomainEventsT'' within ''EnvT''.
 *
 * Note that the `implicit` method has the same name as found in
 * [[com.github.osxhacker.demo.chassis.domain.event.EmitEvents]] so that
 * a compilation error is forced should both be in the inheritance heirarchy.
 */
trait SuppressEvents[EnvT, DomainEventsT <: Coproduct]
	extends PolicyDefinition[EnvT]
{
	/// Implicit Conversions
	final implicit def defaultEventsPolicy[F[_], ChannelT <: Channel] (
		implicit
		applicative : Applicative[F],
		producer : EnvT => EventProducer[F, ChannelT, DomainEventsT]
		)
		: EventPolicy[F, EnvT, DomainEventsT] =
		suppressEventsFor (producer)
}

