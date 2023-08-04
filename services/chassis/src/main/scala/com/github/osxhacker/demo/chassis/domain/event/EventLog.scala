package com.github.osxhacker.demo.chassis.domain.event

import cats.{
	Applicative,
	ApplicativeThrow,
	Functor
	}

import cats.data.{
	Chain,
	WriterT
	}

import shapeless.Coproduct
import shapeless.ops.coproduct.Inject


/**
 * The '''EventLog''' `object` provides "companion object-like" functionality
 * for the [[com.github.osxhacker.demo.chassis.domain.event.EventLog]] `type`.
 */
object EventLog
{
	/// Class Imports
	import cats.syntax.applicativeError._


	/// Class Types
	final class PartiallyAppliedApply[F[_], DomainEventsT <: Coproduct] ()
	{
		def apply[A, EventT] (a : A)
			(event : EventT)
			(
				implicit
				applicative : Applicative[F],
				inject : Inject[DomainEventsT, EventT]
			)
			: EventLog[F, A, DomainEventsT] =
			WriterT.put (a) (Chain (inject (event)))
	}


	final class PartiallyAppliedApplyF[DomainEventsT <: Coproduct] ()
	{
		def apply[F[_], A, EventT] (fa : F[A])
			(event : EventT)
			(
				implicit
				applicative : Applicative[F],
				inject : Inject[DomainEventsT, EventT]
			)
			: EventLog[F, A, DomainEventsT] =
			WriterT.putT (fa) (Chain (inject (event)))
	}


	/**
	 * The apply method employs the "partially applied" idiom to facilitate
	 * creating an
	 * [[com.github.osxhacker.demo.chassis.domain.event.EventLog]] by only
	 * requiring collaborators to provide ''F'' and the ''DomainEventsT'' and
	 * allowing the compiler to derive the rest.
	 */
	def apply[F[_], DomainEventsT <: Coproduct]
		: PartiallyAppliedApply[F, DomainEventsT] =
		new PartiallyAppliedApply[F, DomainEventsT] ()


	/**
	 * The applyF method employs the "partially applied" idiom to facilitate
	 * creating an
	 * [[com.github.osxhacker.demo.chassis.domain.event.EventLog]] by only
	 * requiring collaborators to provide the ''DomainEventsT'' and allowing
	 * the compiler to derive the rest.
	 */
	def applyF[DomainEventsT <: Coproduct]
		: PartiallyAppliedApplyF[DomainEventsT] =
		new PartiallyAppliedApplyF[DomainEventsT] ()


	/**
	 * The liftF method creates an
	 * [[com.github.osxhacker.demo.chassis.domain.event.EventLog]] with an
	 * arbitrary value '''fa''' within the context ''F'' and no events.
	 */
	def liftF[F[_], A, DomainEventsT <: Coproduct] (fa : F[A])
		(implicit applicative : Applicative[F])
		: EventLog[F, A, DomainEventsT] =
		WriterT.liftF[F, Chain[DomainEventsT], A] (fa)


	/**
	 * The pure method creates an
	 * [[com.github.osxhacker.demo.chassis.domain.event.EventLog]] with an
	 * arbitrary value '''a''' and no events.
	 */
	def pure[F[_], A, DomainEventsT <: Coproduct] (a : A)
		(implicit applicative : Applicative[F])
		: EventLog[F, A, DomainEventsT] =
		WriterT.value[F, Chain[DomainEventsT], A] (a)


	/**
	 * The put method creates a new
	 * [[com.github.osxhacker.demo.chassis.domain.event.EventLog]] with both an
	 * initial value '''a''' __and__ the given '''events''' (which can be
	 * empty).
	 */
	def put[F[_], A, DomainEventsT <: Coproduct] (a : A)
		(events : Chain[DomainEventsT])
		(implicit applicative : Applicative[F])
		: EventLog[F, A, DomainEventsT] =
		WriterT.put (a) (events)


	/**
	 * The putT method creates a new
	 * [[com.github.osxhacker.demo.chassis.domain.event.EventLog]] with both an
	 * initial ''F[A]'' '''fa''' __and__ the given '''events''' (which can be
	 * empty).
	 */
	def putT[F[_], A, DomainEventsT <: Coproduct] (fa : F[A])
		(events : Chain[DomainEventsT])
		(implicit functor : Functor[F])
		: EventLog[F, A, DomainEventsT] =
		WriterT.putT (fa) (events)


	/**
	 * The raiseError method creates an
	 * [[com.github.osxhacker.demo.chassis.domain.event.EventLog]] with an
	 * arbitrary '''error''' and no events.
	 */
	def raiseError[F[_], A, DomainEventsT <: Coproduct] (error : Throwable)
		(implicit applicativeThrow : ApplicativeThrow[F])
		: EventLog[F, A, DomainEventsT] =
		liftF (error.raiseError[F, A])
}

