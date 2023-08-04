package com.github.osxhacker.demo.chassis.domain.event

import scala.annotation.{
	implicitNotFound,
	unused
	}

import scala.language.{
	implicitConversions,
	postfixOps
	}

import cats.{
	Applicative,
	Functor,
	Monad
	}

import cats.data.Chain
import shapeless.{
	syntax => _,
	_
	}

import shapeless.ops.coproduct.Inject
import shapeless.ops.hlist._

import com.github.osxhacker.demo.chassis.domain.Specification


/**
 * The '''EventSyntax''' type provides syntactic sugar for using
 * [[com.github.osxhacker.demo.chassis.domain.event.EventConsumer]] and/or
 * [[com.github.osxhacker.demo.chassis.domain.event.EventPolicy]] within a
 * supported container ''F''.
 */
trait EventSyntax
{
	/// Implicit Conversions
	implicit def producerIdOps[F[_], A, EnvT, DomainEventsT <: Coproduct] (
		fa : F[A]
		)
		(
			implicit
			@implicitNotFound ("could not resolve an EventPolicy for EventLog")
			@unused policy : EventPolicy[F, EnvT, DomainEventsT]
		)
		: ProducerIdOps[F, A, DomainEventsT] =
		new ProducerIdOps[F, A, DomainEventsT] (fa)


	implicit def producerLogOps[F[_], A, DomainEventsT <: Coproduct, EnvT] (
		fa : EventLog[F, A, DomainEventsT]
		)
		(
			implicit
			@implicitNotFound ("could not resolve an EventPolicy for EventLog")
			@unused policy : EventPolicy[F, EnvT, DomainEventsT]
		)
		: ProducerLogOps[F, A, DomainEventsT, EnvT] =
		new ProducerLogOps[F, A, DomainEventsT, EnvT] (fa)
}


final class ProducerIdOps[F[_], A, DomainEventsT <: Coproduct] (
	private val self : F[A]
	)
{
	/// Class Imports
	import cats.syntax.semigroup._
	import mouse.boolean._


	/// Class Types
	object LiftEvent
		extends Poly1
	{
		implicit def caseEvent[EventT] (
			implicit

			@implicitNotFound (
				"could not prove ${EventT} is a member of ${DomainEventsT}"
				)
			inject : Inject[DomainEventsT, EventT]
			)
			: Case.Aux[EventT, DomainEventsT] =
			at (inject (_))
	}


	/**
	 * The addEvent method associates the given '''event''' with '''self'''.
	 */
	def addEvent[EventT] (event : EventT)
		(
			implicit
			applicative : Applicative[F],

			@implicitNotFound (
				"could not prove ${EventT} is a member of ${DomainEventsT}"
				)
			inject : Inject[DomainEventsT, EventT]
		)
		: EventLog[F, A, DomainEventsT] =
		EventLog.applyF[DomainEventsT] (self) (event)


	/**
	 * The addEventUnless method associates the given '''event''' with
	 * '''self''', if and only if the '''specification''' evaluates to `false`.
	 */
	def addEventUnless[SpecT <: Specification[A], EventT] (specification : SpecT)
		(event : => EventT)
		(
			implicit
			applicative : Applicative[F],

			@implicitNotFound (
				"could not prove ${EventT} is a member of ${DomainEventsT}"
				)
			inject : Inject[DomainEventsT, EventT]
		)
		: EventLog[F, A, DomainEventsT] =
		addEventWhen (!specification) (event)


	/**
	 * The addEventWhen method associates the given '''event''' with
	 * '''self''', if and only if the '''specification''' evaluates to `true`.
	 */
	def addEventWhen[SpecT <: Specification[A], EventT] (specification : SpecT)
		(event : => EventT)
		(
			implicit
			applicative : Applicative[F],

			@implicitNotFound (
				"could not prove ${EventT} is a member of ${DomainEventsT}"
				)
			inject : Inject[DomainEventsT, EventT]
		)
		: EventLog[F, A, DomainEventsT] =
		EventLog.liftF[F, A, DomainEventsT] (self)
			.mapBoth {
				case (events, a) =>
					(
						events |+| (specification (a) ?? Chain (inject (event))),
						a
					)
				}


	/**
	 * The addEvents method associates zero or more '''events''' with
	 * '''self''', ensuring that each is known to ''DomainEventsT''.
	 */
	def addEvents[InputL <: HList, InjectedL <: HList] (events : InputL)
		(
			implicit
			functor : Functor[F],
			mapper : Mapper.Aux[LiftEvent.type, InputL, InjectedL],
			toTraversable : ToTraversable.Aux[InjectedL, List, DomainEventsT]
		)
		: EventLog[F, A, DomainEventsT] =
		EventLog.putT (self) (Chain.fromSeq (mapper (events).toList))


	/**
	 * The deriveEvent method associates an ''EventT'' derived from the result
	 * of `fa (a)`.
	 */
	def deriveEvent[EventT] (fa : A => EventT)
		(
			implicit
			monad : Monad[F],

			@implicitNotFound (
				"could not prove ${EventT} is a member of ${DomainEventsT}"
				)
			inject : Inject[DomainEventsT, EventT]
		)
		: EventLog[F, A, DomainEventsT] =
		EventLog.liftF (self)
			.flatMap {
				a =>
					EventLog.put (a) (Chain.one (inject (fa (a))))
				}
}


final class ProducerLogOps[F[_], A, DomainEventsT <: Coproduct, EnvT] (
	private val self : EventLog[F, A, DomainEventsT]
	)
{
	/// Class Imports
	import cats.syntax.semigroup._
	import mouse.any._
	import mouse.boolean._


	/// Class Types
	object LiftEvent
		extends Poly1
	{
		implicit def caseEvent[EventT] (
			implicit

			@implicitNotFound (
				"could not prove ${EventT} is a member of ${DomainEventsT}"
				)
			inject : Inject[DomainEventsT, EventT]
			)
			: Case.Aux[EventT, DomainEventsT] =
			at (inject (_))
	}


	/**
	 * The addEvent method associates the given '''event''' with '''self'''.
	 */
	def addEvent[EventT] (event : EventT)
		(
			implicit
			applicative : Applicative[F],

			@implicitNotFound (
				"could not prove ${EventT} is a member of ${DomainEventsT}"
				)
			inject : Inject[DomainEventsT, EventT]
		)
		: EventLog[F, A, DomainEventsT] =
		self.tell (Chain (inject (event)))


	/**
	 * The addEventUnless method associates the given '''event''' with
	 * '''self''', if and only if the '''specification''' evaluates to `false`.
	 */
	def addEventUnless[SpecT <: Specification[A], EventT] (specification : SpecT)
		(event : => EventT)
		(
			implicit
			applicative : Applicative[F],

			@implicitNotFound (
				"could not prove ${EventT} is a member of ${DomainEventsT}"
				)
			inject : Inject[DomainEventsT, EventT]
		)
		: EventLog[F, A, DomainEventsT] =
		addEventWhen (!specification) (event)


	/**
	 * The addEventWhen method associates the given '''event''' with
	 * '''self''', if and only if the '''specification''' evaluates to `true`.
	 */
	def addEventWhen[SpecT <: Specification[A], EventT] (specification : SpecT)
		(event : => EventT)
		(
			implicit
			applicative : Applicative[F],

			@implicitNotFound (
				"could not prove ${EventT} is a member of ${DomainEventsT}"
				)
			inject : Inject[DomainEventsT, EventT]
		)
		: EventLog[F, A, DomainEventsT] =
		self.mapBoth {
			case (events, a) =>
				(
					events |+| (specification (a) ?? Chain (inject (event))),
					a
				)
		}


	/**
	 * The addEvents method associates zero or more '''events''' with
	 * '''self''', ensuring that each is known to ''DomainEventsT''.
	 */
	def addEvents[InputL <: HList, InjectedL <: HList] (events : InputL)
		(
			implicit
			functor : Functor[F],
			mapper : Mapper.Aux[LiftEvent.type, InputL, InjectedL],
			toTraversable : Lazy[ToTraversable.Aux[InjectedL, List, DomainEventsT]]
		)
		: EventLog[F, A, DomainEventsT] =
		self.tell (
			Chain.fromSeq (toTraversable.value (mapper (events)))
			)


	/**
	 * The broadcast method provides an
	 * [[com.github.osxhacker.demo.chassis.domain.event.EventLog]] to the
	 * `implicit`ly resolved '''producer'''.
	 *
	 * @see [[com.github.osxhacker.demo.chassis.domain.event.EventProducer]]
	 */
	def broadcast ()
		(
			implicit
			env : EnvT,
			policy : EventPolicy[F, EnvT, DomainEventsT]
		)
		: F[A] =
		policy (env) (self)


	/**
	 * The broadcastUnless method provides an
	 * [[com.github.osxhacker.demo.chassis.domain.event.EventLog]] to the
	 * `implicit`ly resolved '''producer''' if and only if the given
	 * '''specification''' is __not__ satisfied by the contained ''A''.
	 *
	 * @see [[com.github.osxhacker.demo.chassis.domain.event.EventProducer]]
	 */
	def broadcastUnless (specification : Specification[A])
		(
			implicit
			env : EnvT,
			policy : EventPolicy[F, EnvT, DomainEventsT],
			functor : Functor[F]
		)
		: F[A] =
		broadcastWhen (!specification)


	/**
	 * The broadcastWhen method provides an
	 * [[com.github.osxhacker.demo.chassis.domain.event.EventLog]] to the
	 * `implicit`ly resolved '''producer''' if and only if the given
	 * '''specification''' is satisfied by the contained ''A''.
	 *
	 * @see [[com.github.osxhacker.demo.chassis.domain.event.EventProducer]]
	 */
	def broadcastWhen (specification : Specification[A])
		(
			implicit
			env : EnvT,
			policy : EventPolicy[F, EnvT, DomainEventsT],
			functor : Functor[F]
		)
		: F[A] =
		self.mapBoth {
			case (events, a) =>
				(specification (a) ?? events) -> a
			} |>
			policy (env).apply


	/**
	 * The deriveEvent method associates an ''EventT'' derived from the result
	 * of `fa (a)`.
	 */
	def deriveEvent[EventT] (fa : A => EventT)
		(
			implicit
			monad : Monad[F],

			@implicitNotFound (
				"could not prove ${EventT} is a member of ${DomainEventsT}"
				)
			inject : Inject[DomainEventsT, EventT]
		)
		: EventLog[F, A, DomainEventsT] =
		self.flatMap {
			a =>
				EventLog.put (a) (Chain.one (inject (fa (a))))
			}
}

