package com.github.osxhacker.demo.chassis.domain

import scala.annotation.implicitNotFound
import scala.language.postfixOps

import cats.{
	Endo,
	MonadError,
	Monoid
	}


/**
 * The '''ReaderWriterStateErrorT''' `object` defines a pseudo-companion for the
 * `type` defined in the `package` with the same name.
 *
 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT]]
 */
object ReaderWriterStateErrorT
	extends RWSETFunctions
{
	/// Self Types Constraints
	functions =>


	/// Class Types
	/**
	 * The '''Combinators''' `trait` defines methods which have a reduced
	 * generic parameter signature.
	 *
	 * @see [[com.github.osxhacker.demo.chassis.domain.ReaderWriterStateErrorT.script]]
	 */
	sealed trait Combinators[F[_], EnvT, LogT, ErrorT, S]
	{
		/// Class Types
		final type StateType[A] = ReaderWriterStateErrorT[
			F,
			EnvT,
			LogT,
			ErrorT,
			S,
			A
			]


		/// Instance Properties
		implicit protected def ME : MonadError[F, ErrorT]
		implicit protected def ML : Monoid[LogT]


		/**
		 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.ask]]
		 */
		@inline
		final def ask : StateType[EnvT] = functions ask


		/**
		 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.get]]
		 */
		@inline
		final def get : StateType[S] = functions get


		/**
		 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.inspect]]
		 */
		@inline
		final def inspect[A] (f : S => A) : StateType[A] = functions inspect f


		/**
		 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.inspectAsk]]
		 */
		@inline
		final def inspectAsk[A] (f : (EnvT, S) => A) : StateType[A] =
			functions inspectAsk f


		/**
		 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.inspectAskF]]
		 */
		@inline
		final def inspectAskF[A] (f : (EnvT, S) => F[A]) : StateType[A] =
			functions inspectAskF f


		/**
		 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.inspectF]]
		 */
		@inline
		final def inspectF[A] (f : S => F[A]) : StateType[A] =
			functions inspectF f


		@inline
		final def liftF[A] (fa : F[A]) : StateType[A] = functions liftF fa


		/**
		 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.modify]]
		 */
		@inline
		final def modify (f : Endo[S]) : StateType[Unit] = functions modify f


		/**
		 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.modifyF]]
		 */
		@inline
		final def modifyF (f : S => F[S]) : StateType[Unit] =
			functions modifyF f


		@inline
		final def pure[A] (a : A) : StateType[A] = functions pure a


		/**
		 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.set]]
		 */
		@inline
		final def set (s : S) : StateType[Unit] = functions set s


		/**
		 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.setF]]
		 */
		@inline
		final def setF (sb : F[S]) : StateType[Unit] = functions setF sb


		/**
		 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.tell]]
		 */
		@inline
		final def tell (entries : LogT) : StateType[Unit] =
			functions tell entries


		/**
		 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.tellF]]
		 */
		@inline
		final def tellF (entries : F[LogT]) : StateType[Unit] =
			functions tellF entries
	}


	final class PartiallyAppliedScript[F[_], EnvT, LogT, ErrorT, S] ()
	{
		def apply[A] (
			block : Combinators[F, EnvT, LogT, ErrorT, S] =>
				ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, A]
			)
			(
				implicit

				@implicitNotFound (
					"unable to resolve MonadError[${F}, ${ErrorT}] for apply"
					)
				monadError : MonadError[F, ErrorT],

				@implicitNotFound (
					"unable to resolve Monoid[${LogT}] for apply"
					)
				monoid : Monoid[LogT]
			)
			: ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, A] =
			block (
				new Combinators[F, EnvT, LogT, ErrorT, S] {
					override protected val ME = monadError
					override protected val ML = monoid
					}
				)
	}


	/**
	 * The script method uses the "partially applied" idiom to support creating
	 * [[com.github.osxhacker.demo.chassis.domain.ReaderWriterStateErrorT]]
	 * definitions using those available in
	 * [[com.github.osxhacker.demo.chassis.domain.ReaderWriterStateErrorT.Combinators]].
	 * This technique allows definitions to avoid having to provide every
	 * parameter type needed for
	 * [[com.github.osxhacker.demo.chassis.domain.ReaderWriterStateErrorT]]
	 * in each method use.  For example:
	 *
	 * {{{
	 *     script[IO, MyEnv, Vector[LogEntry], Throwable, NewCompany] {
	 *         combinators =>
	 *             import combinators._
	 *
	 *             for {
	 *                 unsaved <- get
	 *                 _ <- tell (Vector (LogEntry ("saving new company ...")))
	 *                 company <- inspectF[Company] (save)
	 *                 } yield (unsaved, company)
	 *         }
	 * }}}
	 */
	def script[F[_], EnvT, LogT, ErrorT, S]
		: PartiallyAppliedScript[F, EnvT, LogT, ErrorT, S] =
		new PartiallyAppliedScript[F, EnvT, LogT, ErrorT, S] ()
}


sealed trait RWSETFunctions
{
	final def ask[F[_], EnvT, LogT, ErrorT, S]
		(
			implicit

			@implicitNotFound (
				"unable to resolve MonadError[${F}, ${ErrorT}] for ask"
				)
			monadError : MonadError[F, ErrorT],

			@implicitNotFound ("unable to resolve Monoid[${LogT}] for ask")
			monoid : Monoid[LogT]
		)
		: ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, EnvT] =
		IndexedReaderWriterStateErrorT ask


	final def get[F[_], EnvT, LogT, ErrorT, S]
		(
			implicit

			@implicitNotFound (
				"unable to resolve MonadError[${F}, ${ErrorT}] for get"
				)
			monadError : MonadError[F, ErrorT],

			@implicitNotFound ("unable to resolve Monoid[${LogT}] for get")
			monoid : Monoid[LogT]
		)
		: ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, S] =
		IndexedReaderWriterStateErrorT get


	final def inspect[F[_], EnvT, LogT, ErrorT, S, A] (f : S => A)
		(
			implicit

			@implicitNotFound (
				"unable to resolve MonadError[${F}, ${ErrorT}] for inspect"
				)
			monadError : MonadError[F, ErrorT],

			@implicitNotFound ("unable to resolve Monoid[${LogT}] for inspect")
			monoid : Monoid[LogT]
		)
		: ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, A] =
		IndexedReaderWriterStateErrorT inspect f


	final def inspectAsk[F[_], EnvT, LogT, ErrorT, S, A] (f : (EnvT, S) => A)
		(
			implicit

			@implicitNotFound (
				"unable to resolve MonadError[${F}, ${ErrorT}] for inspectAsk"
				)
			monadError : MonadError[F, ErrorT],

			@implicitNotFound (
				"unable to resolve Monoid[${LogT}] for inspectAsk"
				)
			monoid : Monoid[LogT]
		)
		: ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, A] =
		IndexedReaderWriterStateErrorT inspectAsk f


	final def inspectAskF[F[_], EnvT, LogT, ErrorT, S, A] (
		f : (EnvT, S) => F[A]
		)
		(
			implicit

			@implicitNotFound (
				"unable to resolve MonadError[${F}, ${ErrorT}] for inspectAskF"
				)
			monadError : MonadError[F, ErrorT],

			@implicitNotFound (
				"unable to resolve Monoid[${LogT}] for inspectAskF"
				)
			monoid : Monoid[LogT]
		)
		: ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, A] =
		IndexedReaderWriterStateErrorT inspectAskF f


	final def inspectF[F[_], EnvT, LogT, ErrorT, S, A] (f : S => F[A])
		(
			implicit

			@implicitNotFound (
				"unable to resolve MonadError[${F}, ${ErrorT}] for inspectF"
				)
			monadError : MonadError[F, ErrorT],

			@implicitNotFound ("unable to resolve Monoid[${LogT}] for inspectF")
			monoid : Monoid[LogT]
		)
		: ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, A] =
		IndexedReaderWriterStateErrorT inspectF f


	/**
	 * The liftF method attempts to incorporate the given '''fa''' instance as
	 * the result of a new
	 * [[com.github.osxhacker.demo.chassis.domain.ReaderWriterStateErrorT]]
	 * instance.  Errors represented by '''fa''' are supported.
	 */
	final def liftF[F[_], EnvT, LogT, ErrorT, S, A] (fa : F[A])
		(
			implicit

			@implicitNotFound (
				"unable to resolve MonadError[${F}, ${ErrorT}] for liftF"
				)
			monadError : MonadError[F, ErrorT],

			@implicitNotFound ("unable to resolve Monoid[${LogT}] for liftF")
			monoid : Monoid[LogT]
		)
		: ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, A] =
		IndexedReaderWriterStateErrorT liftF fa


	final def modify[F[_], EnvT, LogT, ErrorT, S] (f : Endo[S])
		(
			implicit

			@implicitNotFound (
				"unable to resolve MonadError[${F}, ${ErrorT}] for modify"
				)
			monadError : MonadError[F, ErrorT],

			@implicitNotFound ("unable to resolve Monoid[${LogT}] for modify")
			monoid : Monoid[LogT]
		)
		: ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, Unit] =
		IndexedReaderWriterStateErrorT modify f


	final def modifyF[F[_], EnvT, LogT, ErrorT, S] (f : S => F[S])
		(
			implicit

			@implicitNotFound (
				"unable to resolve MonadError[${F}, ${ErrorT}] for modifyF"
				)
			monadError : MonadError[F, ErrorT],

			@implicitNotFound ("unable to resolve Monoid[${LogT}] for modifyF")
			monoid : Monoid[LogT]
		)
		: ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, Unit] =
		IndexedReaderWriterStateErrorT modifyF f


	final def pure[F[_], EnvT, LogT, ErrorT, S, A] (a : A)
		(
			implicit

			@implicitNotFound (
				"unable to resolve MonadError[${F}, ${ErrorT}] for pure"
				)
			monadError : MonadError[F, ErrorT],

			@implicitNotFound ("unable to resolve Monoid[${LogT}] for pure")
			monoid : Monoid[LogT]
		)
		: ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, A] =
		IndexedReaderWriterStateErrorT pure a


	final def set[F[_], EnvT, LogT, ErrorT, S] (s : S)
		(
			implicit

			@implicitNotFound (
				"unable to resolve MonadError[${F}, ${ErrorT}] for set"
				)
			monadError : MonadError[F, ErrorT],

			@implicitNotFound ("unable to resolve Monoid[${LogT}] for set")
			monoid : Monoid[LogT]
		)
		: ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, Unit] =
		IndexedReaderWriterStateErrorT set s


	final def setF[F[_], EnvT, LogT, ErrorT, S] (s : F[S])
		(
			implicit

			@implicitNotFound (
				"unable to resolve MonadError[${F}, ${ErrorT}] for setF"
				)
			monadError : MonadError[F, ErrorT],

			@implicitNotFound ("unable to resolve Monoid[${LogT}] for setF")
			monoid : Monoid[LogT]
		)
		: ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, Unit] =
		IndexedReaderWriterStateErrorT setF s


	final def tell[F[_], EnvT, LogT, ErrorT, S] (entries : LogT)
		(
			implicit

			@implicitNotFound (
				"unable to resolve MonadError[${F}, ${ErrorT}] for tell"
				)
			monadError : MonadError[F, ErrorT]
		)
		: ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, Unit] =
		IndexedReaderWriterStateErrorT tell entries


	final def tellF[F[_], EnvT, LogT, ErrorT, S] (entries : F[LogT])
		(
			implicit

			@implicitNotFound (
				"unable to resolve MonadError[${F}, ${ErrorT}] for tellF"
				)
			monadError : MonadError[F, ErrorT],

			@implicitNotFound ("unable to resolve Monoid[${LogT}] for tellF")
			monoid : Monoid[LogT]
		)
		: ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, Unit] =
		IndexedReaderWriterStateErrorT tellF entries
}

