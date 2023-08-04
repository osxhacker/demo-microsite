package com.github.osxhacker.demo.chassis.domain.event

import cats.data.Kleisli


/**
 * The '''EventConsumer''' type defines the Domain Object Model ability to
 * consume an arbitrary number of events from the specified ''ChannelT''.  The
 * '''EventConsumer''' is expected to complete when either all events have been
 * consumed or the environment indicates consumption should cease.
 *
 * Any recoverable errors encountered during transmission must be addressed by
 * the given `interpreter`.  Unrecoverable errors will be represented in a
 * failed ''F'', which will also terminate event consumption.
 */
abstract class EventConsumer[F[_], ChannelT <: Channel, EventT, EnvT] (
	val channel : ChannelT
	)
{
	/**
	 * This version of the apply method is provided for syntactic convenience by
	 * lifting the given '''interpreter''' into a [[cats.data.Kleisli]].
	 */
	final def apply (interpreter : (EventT, EnvT) => F[Unit]) : F[Unit] =
		apply (Kleisli (interpreter.tupled))


	/**
	 * This version of the apply method is expected to implement event
	 * processing in terms of the provided '''interpreter'''.
	 */
	def apply (interpreter : Kleisli[F, (EventT, EnvT), Unit]) : F[Unit]
}

