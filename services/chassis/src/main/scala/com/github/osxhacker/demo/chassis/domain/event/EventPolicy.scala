package com.github.osxhacker.demo.chassis.domain.event

import shapeless.Coproduct


/**
 * The '''EventPolicy''' type defines the contract for resolving an
 * [[com.github.osxhacker.demo.chassis.domain.event.EventProducer]] from a known
 * ''EnvT'' __and__ determines how, if at all, ''DomainEventsT'' are published.
 * Only one [[com.github.osxhacker.demo.chassis.domain.event.EventProducer]] can
 * exist for any ''EnvT'' / ''DomainEventsT'' pair.
 *
 * @see [[com.github.osxhacker.demo.chassis.domain.event.PolicyDefinition]]
 */
trait EventPolicy[F[_], EnvT, DomainEventsT <: Coproduct]
{
	type ChannelType <: Channel


	def apply (env : EnvT) : EventProducer[F, ChannelType, DomainEventsT]
}

