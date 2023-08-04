package com.github.osxhacker.demo.chassis.effect

import scala.language.postfixOps

import cats.data.{
	Kleisli,
	StateT
	}

import cats.effect._
import cats.effect.std.Semaphore


/**
 * The '''ReadersWriterResource''' type provides
 * [[https://en.wikipedia.org/wiki/Readers%E2%80%93writer_lock Readers Writer Lock]]
 * based access to an underlying ''ManagedT'' instance.
 *
 * ==Overview==
 *
 * There are two variants of usage available.
 *
 * When read-only use of the ''ManagedT'' instance is desired, use a `reader`
 * method without worry of a `writer` changing the instance.  Before the
 * `reader` logic executes, the `numberOfReaders` is incremented and the
 * `semaphore` released.  When the `reader` logic completes, the
 * `numberOfReaders` is automatically decremented.
 *
 * To affect change of the ''ManagedT'' instance, the `writer` methods will
 * only execute when there are no active `reader`s.  By holding the lock
 * throughout the write operation, no `reader` can erroneously use a (soon to
 * be) stale ''ManagedT'' instance.
 *
 * Note that while this type is thread safe, no such guarantees are extended to
 * ''ManagedT''.  Therefore, any mutable state in ''ManagedT'' must be managed
 * accordingly.  Where possible, a ''ManagedT'' type which is immutable is
 * recommended.
 *
 * ==Implementation Notes==
 *
 * In order to minimize assumptions regarding ''F'', the requisite collaborators
 * are provided during construction.  Having them as member `val`ues creates the
 * possibility of not being able to guarantee one shared instance of each.
 *
 * This was observed when testing with [[cats.effect.IO]], as that container
 * defines the __description of the steps__ to execute and not executing them
 * when defined.
 */
final class ReadersWriterResource[F[_], ManagedT] private (
	private val instance : Ref[F, ManagedT],
	private val semaphore : Semaphore[F],
	private val numberOfReaders : Ref[F, Int]
	)
	(implicit private val sync : Sync[F])
{
	/// Class Imports
	import cats.effect.syntax.monadCancel._
	import cats.syntax.eq._
	import cats.syntax.flatMap._
	import cats.syntax.functor._


	/**
	 * This version of the reader method creates a [[cats.data.Kleisli]] having
	 * the given '''f'''unctor and delegates evaluation to the
	 * [[cats.data.Kleisli]]-based `reader`.  Any changes made to the given
	 * ''ManageT'' '''instance''' are discarded.
	 *
	 * No `writer` is allowed to acquire the '''semaphore''' until there are no
	 * active `reader`s.
	 */
	def reader[A] (f : ManagedT => F[A]) : F[A] = reader (Kleisli (f))


	/**
	 * This version of the reader method uses [[cats.effect.MonadCancel]] to
	 * `bracket` the incrementing and decrementing of the `numberOfReaders`
	 * between invocation of the '''kleisli'''.  Any changes made to the given
	 * ''ManageT'' '''instance''' are discarded.
	 *
	 * No `writer` is allowed to acquire the '''semaphore''' until there are no
	 * active `reader`s.
	 */
	def reader[A] (kleisli : Kleisli[F, ManagedT, A]) : F[A] =
		(incrementReaders () >> instance.get).bracket (kleisli (_)) (
			_ => decrementReaders ()
			)


	/**
	 * This version of the writer method uses the given '''f'''unctor to
	 * atomically alter the managed '''instance'''.  It is not invoked until the
	 * '''semaphore''' is `acquire`d when there are no active `reader`s.  If the
	 * '''f'''unctor completes successfully, the contained ''ManagedT'' instance
	 * becomes available for use.  Should ''F'' contain an error, the prior
	 * '''instance''' is unmodified.  In both cases, the '''semaphore''' is
	 * released.
	 *
	 * No `reader` or `writer` is allowed to acquire the '''semaphore''' until
	 * there is no active `writer`.
	 */
	def writer (f : ManagedT => F[ManagedT]) : F[ManagedT] =
		writer (Kleisli (f))


	/**
	 * This version of the writer method uses the given '''kleisli''' to
	 * atomically alter the managed '''instance'''.  It is not invoked until the
	 * '''semaphore''' is `acquire`d when there are no active `reader`s.  If the
	 * '''kleisli''' completes successfully, the updated ''ManagedT'' instance
	 * becomes available for use.  Should ''F'' contain an error, the prior
	 * '''instance''' is unmodified.  In both cases, the '''semaphore''' is
	 * released.
	 *
	 * No `reader` or `writer` is allowed to acquire the '''semaphore''' until
	 * there is no active `writer`.
	 */
	def writer (kleisli : Kleisli[F, ManagedT, ManagedT]) : F[ManagedT] =
		(acquireWriteLock () >> instance.get).bracket {
			kleisli (_).flatTap (instance.set)
			} (_ => releaseWriteLock ())


	/**
	 * This version of the writer method uses the given '''steps''' to
	 * atomically alter the managed '''instance'''.  It is not invoked until the
	 * '''semaphore''' is `acquire`d when there are no active `reader`s.  If the
	 * [[cats.data.StateT]] completes successfully, the updated ''ManagedT''
	 * instance becomes available for use.  Should ''F'' contain an error, the
	 * prior '''instance''' is unmodified.  In both cases, the '''semaphore'''
	 * is released.
	 *
	 * No `reader` or `writer` is allowed to acquire the '''semaphore''' until
	 * there is no active `writer`.
	 */
	def writer[A] (steps : StateT[F, ManagedT, A]) : F[A] =
		acquireWriteLock ().bracket {
			_ =>
				for {
					initial <- instance.get
					result <- steps.run (initial)
					_ <- instance.set (result._1)
					} yield result._2
			} (_ => releaseWriteLock ())


	private def acquireWriteLock () : F[Unit] =
	{
		/// While this helper method is defined to be recursive, it is not
		/// "stack recursive" due to how `Sync.flatMap` is expected to be
		/// implemented.
		def waitForNoReaders (poll : Poll[F]) : F[Unit] =
			(poll (semaphore.acquire) >> numberOfReaders.get)
				.map (_ === 0)
				.ifM (
					sync.unit,
					semaphore.release >> waitForNoReaders (poll)
					)

		sync.uncancelable (waitForNoReaders)
	}


	private def adjustNumberOfReaders (delta : Int) : F[Unit] =
		sync.uncancelable {
			poll =>
				for {
					_ <- poll (semaphore.acquire)
					unit <- numberOfReaders.update (_ + delta)
					_ <- semaphore.release
				} yield unit
			}


	private def decrementReaders () : F[Unit] = adjustNumberOfReaders (-1)


	private def incrementReaders () : F[Unit] = adjustNumberOfReaders (1)


	private def releaseWriteLock () : F[Unit] = semaphore.release
}


object ReadersWriterResource
{
	/**
	 * The from method creates a '''ReadersWriterResource''' with an arbitrary
	 * '''instance''' within the container ''F''.
	 */
	def from[F[_], A] (instance : A)
		(
			implicit
			genConcurrent : GenConcurrent[F, Throwable],
			make : Ref.Make[F],
			sync : Sync[F]
		)
		: F[ReadersWriterResource[F, A]] =
		sync.flatMap (Ref.of[F, A] (instance)) (fromRef[F, A])


	/**
	 * The fromDeferred method creates a '''ReadersWriterResource''' from a
	 * '''deferred''' instance within the container ''F''.  Note that control
	 * will not return to the caller until '''deferred''' has been `complete`d.
	 */
	def fromDeferred[F[_], A] (deferred : Deferred[F, Ref[F, A]])
		(
			implicit
			genConcurrent : GenConcurrent[F, Throwable],
			make : Ref.Make[F],
			sync : Sync[F]
		)
		: F[ReadersWriterResource[F, A]] =
		sync.flatMap (deferred.get) (fromRef[F, A])


	/**
	 * The fromRef method creates a '''ReadersWriterResource''' with the given
	 * '''ref''' within the container ''F''.
	 */
	def fromRef[F[_], A] (ref : Ref[F, A])
		(
			implicit
			genConcurrent : GenConcurrent[F, Throwable],
			make : Ref.Make[F],
			sync : Sync[F]
		)
		: F[ReadersWriterResource[F, A]] =
		sync.map2 (Semaphore[F] (1), Ref.of[F, Int] (0)) (
			new ReadersWriterResource[F, A] (ref, _, _)
			)
}

