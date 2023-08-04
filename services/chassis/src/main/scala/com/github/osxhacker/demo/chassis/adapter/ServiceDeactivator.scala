package com.github.osxhacker.demo.chassis.adapter

import java.time.OffsetDateTime

import cats.MonadThrow
import cats.effect.{
	Async,
	Deferred,
	GenConcurrent
	}

import eu.timepit.refined
import eu.timepit.refined.api.Refined


/**
 * The '''ServiceDeactivator''' type defines a synchronization mechanism to
 * indicate the desire for a microservice to stop interacting with external
 * stimuli.  It does so by managing a [[cats.effect.Deferred]] instance having
 * information describing why and when the deactivation was requested.
 */
final class ServiceDeactivator[F[_]] (
	private val context : Deferred[F, ServiceDeactivator.Context]
	)
	(
		implicit
		/// Needed for `flatMap` and `liftTo`.
		monadError : MonadThrow[F]
	)
{
	/// Class Imports
	import ServiceDeactivator.Context
	import ServiceDeactivator.Context.Reason
	import cats.syntax.either._
	import cats.syntax.flatMap._
	import cats.syntax.functor._
	import mouse.any._


	/// Instance Properties
	private val fusedFOption = monadError.compose[Option]


	/**
	 * The await method blocks until the `context` has been `complete`d by a
	 * different execution flow.  If `context` has already been provided, await
	 * returns with the reason / when pair.
	 */
	def await () : F[(String, OffsetDateTime)] =
		context.get
			.map (mkPair)


	/**
	 * The signal method attempts to `complete` the `context` with the given
	 * [[eu.timepit.refined.api.Refined]] '''reason''' as to why the
	 * deactivation is being requested.  The returned ''F'' indicates whether or
	 * not this invocation is the one which modified the `context` and may
	 * contain an error if the '''reason''' is unacceptable.
	 *
	 * Any [[cats.effect.Fiber]]s (or equivalent) blocked awaiting the `context`
	 * completion will be notified.
	 *
	 * Conversions from constant ''String''s and __compatible__
	 * [[eu.timepit.refined.api.Refined]] types is supported by leveraging the
	 * [[eu.timepit.refined.auto]] support.
	 */
	def signal (reason : Reason) : F[Boolean] =
		Context (reason, OffsetDateTime.now ()) |> context.complete


	/**
	 * The tryGet method attempts to produce the reason and when it was given if
	 * it has already been `signal`led.  If the `context` has not been
	 * `complete`d, tryGet immediately returns ''None''.
	 */
	def tryGet () : F[Option[(String, OffsetDateTime)]] =
		fusedFOption.map (context.tryGet) (mkPair)


	/**
	 * The trySignal method attempts to `complete` the `context`
	 * with the given unvalidated '''reason''' as to why the deactivation is
	 * being requested.  The returned ''F'' indicates whether or not this
	 * invocation is the one which modified the `context` and may contain an
	 * error if the '''reason''' is unacceptable.
	 *
	 * Any [[cats.effect.Fiber]]s (or equivalent) blocked awaiting the `context`
	 * completion will be notified if the '''reason''' is accepted.
	 */
	def trySignal (reason : String) : F[Boolean] =
		Context.from (reason, OffsetDateTime.now ())
			.liftTo[F]
			.flatMap (context.complete)


	private def mkPair (instance : Context) =
		instance.reason.value -> instance.when
}


object ServiceDeactivator
{
	/// Class Imports
	import cats.syntax.functor._


	final case class Context (
		val reason : Context.Reason,
		val when : OffsetDateTime
		)


	object Context
	{
		/// Class Imports
		import cats.syntax.bifunctor._
		import refined.boolean.And
		import refined.collection.{
			MaxSize,
			MinSize
			}

		import refined.api.RefType


		/// Class Types
		/**
		 * The '''Reason''' [[eu.timepit.refined.api.Refined]] type ensures the
		 * underlying ''String'' has content which is compatible with API
		 * shutdown messages.
		 */
		type Reason = Refined[
			String,
			MinSize[1] And MaxSize[1024]
			]


		/**
		 * This version of the from method attempts to create a '''Context'''
		 * instance with an (unvalidated) '''reason'''.
		 */
		def from (reason : String) : Either[IllegalArgumentException, Context] =
			from (reason, OffsetDateTime.now ())


		/**
		 * This version of the from method attempts to create a '''Context'''
		 * instance with the (unvalidated) '''reason''' and '''when''' it was
		 * provided.
		 */
		def from (reason : String, when : OffsetDateTime)
			: Either[IllegalArgumentException, Context] =
			RefType.applyRef[Reason] (reason)
				.bimap (
					new IllegalArgumentException (_),
					Context (_, when)
					)
	}


	/**
	 * The apply method provides functional-style '''ServiceDeactivator'''
	 * creation within the container ''F''.
	 */
	def apply[F[_]] ()
		(implicit genConcurrent : GenConcurrent[F, Throwable])
		: F[ServiceDeactivator[F]] =
		Deferred[F, Context] map (new ServiceDeactivator[F] (_))


	/**
	 * The unsafe method allocates a '''ServiceDeactivator''' directly, instead
	 * of doing so within a container.  Prefer using `apply` when possible.
	 */
	def unsafe[F[_]] ()
		(implicit async : Async[F])
		: ServiceDeactivator[F] =
		new ServiceDeactivator[F] (Deferred.unsafe[F, Context])
}

