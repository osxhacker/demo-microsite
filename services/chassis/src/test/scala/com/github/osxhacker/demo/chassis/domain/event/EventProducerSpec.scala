package com.github.osxhacker.demo.chassis.domain.event

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.diagrams.Diagrams
import shapeless.{
	syntax => _,
	_
	}

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.chassis.domain.{
	ErrorOr,
	Specification
	}


/**
 * The '''EventProducerSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.domain.event.EventProducer]] for fitness
 * of purpose and serves as an exemplar of its use.
 */
final class EventProducerSpec ()
	extends AnyWordSpec
		with Diagrams
		with ProjectSpec
		with EventSupport
		with EmitEvents[
			EventProducerSpec.ProducerEnvironment,
			EventProducerSpec.SampleEvents
			]
{
	/// Class Imports
	import EventProducerSpec._
	import cats.syntax.applicative._
	import syntax._

	/// Instance Properties
	implicit val env = ProducerEnvironment ()


	"The EventProducer concept" must {
		"be able to create an event log with one event from an F[A]" in withProducer {
			implicit producer =>
				val fa : ErrorOr[Int] = 99.pure[ErrorOr]
				val log = fa.addEvent (EventB ())
				val result = producer (log)

				assert (result.isRight)
				assert (producer.emitted.nonEmpty)
			}

		"be able to create an event log with multiple events from an F[A]" in withProducer {
			implicit producer =>
				val fa = 99.pure[ErrorOr]
				val log = fa.addEvents (EventB () :: EventA () :: HNil)
				val result = producer (log)

				assert (result.isRight)
				assert (producer.emitted.size === 2)
			}

		"be able to add to the log across log instances" in withProducer {
			implicit producer =>
				val flow = 1.pure[ErrorOr]
					.addEvent (EventA ())
					.flatMap {
						_ =>
							EventLog.pure (2)
						}
					.addEvent (EventB ())

				val result = producer (flow)

				assert (result.isRight)
				assert (producer.emitted.size === 2)
			}

		"be able to conditionally add events to a log" in withProducer {
			implicit producer =>
				val fa = 99.pure[ErrorOr]
				val allowed = fa.addEventWhen (Specification[Int] (_ >= 0)) (
					EventA ()
					)
					.broadcast ()

				val disallowed = fa.addEventUnless (Specification[Int] (_ >= 0)) (
					EventB ()
					)
					.broadcast ()

				assert (allowed.isRight)
				assert (disallowed.isRight)
				assert (producer.emitted.size === 1)
			}

		"not produce a log when F[A] contains an error" in withProducer {
			implicit producer =>
				val flow = 99.pure[ErrorOr]
					.addEvent (EventA ())
					.flatMap {
						_ =>
							EventLog.raiseError[ErrorOr, Int, SampleEvents] (
								new Exception ("simulated")
								)
						}
					.addEvent (EventB ())

				val result = producer (flow)

				assert (result.isLeft)
				assert (producer.emitted.isEmpty)
			}
		}


	private def withProducer[A] (
		testSpec : MockEventProducer[ErrorOr, SampleChannel, SampleEvents] => A
		)
		: A =
		testSpec (createEventProducer[ErrorOr, SampleEvents] ())
}


object EventProducerSpec
{
	/// Class Types
	final type SampleEvents =
		EventA :+:
		EventB :+:
		CNil


	final case class ProducerEnvironment ()


	object ProducerEnvironment
	{
		implicit def resolveProducer[
			F[_],
			ChannelT <: Channel,
			DomainEventsT <: Coproduct
			] (implicit producer : EventProducer[F, ChannelT, DomainEventsT])
			: ProducerEnvironment => EventProducer[F, ChannelT, DomainEventsT] =
			_ => producer
	}

	final case class EventA ()


	final case class EventB ()
}

