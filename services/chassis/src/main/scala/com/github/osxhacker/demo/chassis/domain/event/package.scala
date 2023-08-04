package com.github.osxhacker.demo.chassis.domain

import cats.data.{Chain, WriterT}
import shapeless.Coproduct


/**
 * ==Overview==
 *
 * The '''event''' `package` defines common types responsible for consuming and
 * emitting [[https://dzone.com/articles/what-are-domain-events domain events]].
 */
package object event
{
	/// Class Types
	/**
	 * The '''EventLog''' type defines the contract for accumulating zero or
	 * more ''DomainEventsT'' within the context ''F'' for subsequent emission.
	 */
	type EventLog[F[_], A, DomainEventsT <: Coproduct] = WriterT[
		F,
		Chain[DomainEventsT],
		A
		]


	object syntax
		extends EventSyntax
}

