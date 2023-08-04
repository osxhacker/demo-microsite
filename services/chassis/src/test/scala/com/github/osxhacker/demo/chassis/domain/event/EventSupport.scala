package com.github.osxhacker.demo.chassis.domain.event

import scala.collection.mutable

import cats.ApplicativeThrow
import enumeratum._
import shapeless.Coproduct
import com.github.osxhacker.demo.chassis.ProjectSpec


/**
 * The '''EventSupport''' type provides supporting behaviour used in simulating
 * domain event usage.
 */
trait EventSupport
{
	/// Self Type Constraints
	this : ProjectSpec =>


	/// Class Imports
	import cats.syntax.functor._


	/// Class Types
	final case class MockEventProducer[
		F[_],
		ChannelT <: Channel,
		DomainEventsT <: Coproduct
		] (
			override val channel : ChannelT,
			val emitted : mutable.ListBuffer[DomainEventsT]
		)
		(implicit private val applicativeThrow : ApplicativeThrow[F])
		extends EventProducer[F, ChannelT, DomainEventsT] (channel)
	{
		def this (channel : ChannelT)
			(implicit applicativeThrow : ApplicativeThrow[F])
			= this (channel, new mutable.ListBuffer[DomainEventsT] ())


		override def apply[A] (events : EventLog[F, A, DomainEventsT]) : F[A] =
			events.run
				.map {
					case (pending, a) =>
						emitted.appendAll (pending.toList)
						a
				}


		override def createChannel () : F[Unit] = applicativeThrow.unit
	}


	sealed trait SampleChannel
		extends Channel


	object SampleChannel
		extends Enum[SampleChannel]
	{
		case object Default
			extends SampleChannel


		/// Instance Properties
		val values = findValues
	}


	/**
	 * The createEventProducer method instantiates a '''MockEventProducer'''
	 * which has no previous ''DomainEventsT'' in its `emitted` buffer.
	 */
	protected def createEventProducer[F[_], DomainEventsT <: Coproduct] ()
		(implicit applicativeThrow : ApplicativeThrow[F])
		: MockEventProducer[F, SampleChannel, DomainEventsT] =
		new MockEventProducer[F, SampleChannel, DomainEventsT] (
			SampleChannel.Default
			)
}

