package com.github.osxhacker.demo.chassis.adapter

import cats.data.{
	EitherT,
	Kleisli,
	OptionT
	}


/**
 * The '''kafka''' `package` defines types responsible for publishing and
 * consuming [[https://kafka.apache.org/books-and-papers Apache Kafka]]
 * messages.
 */
package object kafka
{
	/// Class Types
	/**
	 * The '''EventProcessorContainer''' type defines the container ("''F[_]''")
	 * used by
	 * [[com.github.osxhacker.demo.chassis.adapter.kafka.EventProcessor]].
	 */
	type EventProcessorContainer[F[_], A] = EitherT[OptionT[F, *], Throwable, A]


	/**
	 * The '''EventProcessor''' type defines the contract for logic which
	 * filters and/or transforms events in the steps for consuming Kafka events.
	 * What this provides to the system is the ability to define event
	 * consumption in terms of the
	 * [[https://en.wikipedia.org/wiki/Chain-of-responsibility_pattern Chain-of-responsibility pattern]].
	 */
	type EventProcessor[F[_], -InT, OutT] = Kleisli[
		EventProcessorContainer[F, *],
		InT,
		OutT
		]
}

