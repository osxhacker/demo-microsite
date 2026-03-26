package com.github.osxhacker.demo.chassis.domain

import scala.annotation.{
	implicitNotFound,
	unused
	}

import scala.language.postfixOps

import cats._
import cats.data.EitherT


/**
 * The '''IndexedReaderWriterStateErrorT''' type defines a stateful computation
 * within the context ''F[_]'', from state ''SA'' to ''SB'', with an
 * environment ''EnvT'', an accumulated log of type ''LogT'', and resulting in
 * ''LogT'' and __either__ an error of type ''ErrorT'' or the result of the
 * computation.  An important design decision enforced is __all__ operations are
 * implemented such that results are woven through ''F[_]'' sequentially to
 * ensure no copies of ''F[_]'' are produced.  Furthermore, additions to the
 * managed ''LogT'' are disallowed once the __first__ error is detected in
 * ''F[_]''.
 *
 * '''IndexedReaderWriterStateErrorT''' is conceptually similar to
 * [[cats.data.IndexedReaderWriterStateT]], with the primary differences being
 * ''LogT'' is always produced and the intrinsic ability to represent errors.
 * As such, the behavior provided by this type is very similar as well
 * (including the omission of nullary argument specifications).
 */
final class IndexedReaderWriterStateErrorT[F[_], EnvT, LogT, ErrorT, SA, SB, A] (
	private[chassis] val runE : (EnvT, SA) =>
		EitherT[F, (LogT, ErrorT), (LogT, SB, A)]
	)
	(implicit private val monadError : MonadError[F, ErrorT])
	extends Serializable
{
	/// Class Imports
	import cats.syntax.all._


	/**
	 * The adaptError method provides the ability to selectively transform
	 * ''ErrorT'' instances when '''this''' instance represents an error state.
	 */
	def adaptError (pf : PartialFunction[ErrorT, ErrorT])
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, A] =
		IndexedReaderWriterStateErrorT {
			runE  (_, _) leftMap (_ map (pf applyOrElse (_, identity[ErrorT])))
			}


	/**
	 * The ask method retrieves ''EnvT'' from this instance.
	 */
	def ask
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, EnvT] =
		IndexedReaderWriterStateErrorT {
			(env, sa) =>
				runE (env, sa) map {
					case (l, sb, _) =>
						(l, sb, env)
					}
			}


	/**
	 * The bimap method modifies the existing state ''SB'' with '''fs''' and
	 * the result of the computation ''A'' with '''fa'''.
	 */
	def bimap[SC, B] (fs : SB => SC, fa : A => B)
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SC, B] =
		transform ((l, sb, a) => (l, fs (sb), fa (a)))


	/**
	 * The contramap method allows for the definition of an
	 * '''IndexedReaderWriterStateErrorT''' having an initial state of ''S0'',
	 * from which a ''SA'' can be derived.
	 */
	def contramap[S0] (f : S0 => SA)
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S0, SB, A] =
		IndexedReaderWriterStateErrorT ((env, s0) => runE (env, f (s0)))


	/**
	 * The dimap method transforms the initial state of ''S0'' to ''SA''
	 * __and then__ ''SB'' to ''S1'' as a single operation.
	 */
	def dimap[S0, S1] (f : S0 => SA)
		(g : SB => S1)
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S0, S1, A] =
		contramap (f) modify g


	/**
	 * The flatMap method uses the result of '''this''' instance to produce a
	 * new '''IndexedReaderWriterStateErrorT''' if '''this''' does not represent
	 * an error, having the combined logs and potentially a new state type
	 * ''SC''.  Any existing or raised errors are represented in the return
	 * value.
	 */
	def flatMap[SC, B] (
		f : A => IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SB, SC, B]
		)
		(
			implicit

			@implicitNotFound (
				"${LogT} must have a Semigroup defined and in the implicit scope"
				)
			semigroup : Semigroup[LogT]
		)
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SC, B] =
		IndexedReaderWriterStateErrorT {
			(env, sa) =>
				runE (env, sa) flatMap {
					case (log, sb, a) =>
						f (a).runE (env, sb)
							.bimap (
								l => (log |+| l._1) -> l._2,
								r => (log |+| r._1, r._2, r._3)
								)
					}
			}


	/**
	 * The flatMapF method is similar to `map`, with difference being '''f'''
	 * results in a value ''B'' within the context ''F[_]''.  Errors within the
	 * produced ''F[B]'' are represented in the return value.
	 */
	def flatMapF[B] (f : A => F[B])
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, B] =
		transformF ((l, sb, a) => f (a) map (b => (l, sb, b)))


	/**
	 * The get method retrieves ''SB'' from this instance and makes it available
	 * in the result.
	 */
	def get
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, SB] =
		inspect (identity)


	/**
	 * The handleErrorWith method handles any ''ErrorT'' by producing an
	 * '''IndexedReaderWriterStateErrorT''' based on the ''ErrorT'' contained in
	 * '''this''' instance via the given '''f''' functor.  If '''this''' is not
	 * in an error condition, '''f''' will not be evaluated.  Note that the
	 * `handleError` method is __not__ defined in this type due to ''SA'' and
	 * ''SB'' being potentially different types.
	 */
	def handleErrorWith (
		f : ErrorT =>
			IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, A]
		)
		(
			implicit

			@implicitNotFound (
				"unable to resolve Semigroup[${LogT}] for handleErrorWith"
				)
			semigroup : Semigroup[LogT]
		)
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, A] =
		IndexedReaderWriterStateErrorT {
			(env, sa) =>
				runE (env, sa) handleErrorWith {
					case (log, error) =>
						f (error).runE (env, sa)
							.bimap (
								le => (log |+| le._1) -> le._2,
								lsa => (log |+| lsa._1, lsa._2, lsa._3)
								)
					}
				}


	/**
	 * The inspect method provides the resulting state ''SB'' to the given
	 * functor '''f''' and made available in the result.
	 */
	def inspect[B] (f : SB => B)
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, B] =
		transform ((l, sb, _) => (l, sb, f (sb)))


	/**
	 * The inspectAsk method provides both the environment ''EnvT'' and
	 * resulting state ''SB'' to the given functor '''f''', whose value ''B'' is
	 * made available in the result.
	 */
	def inspectAsk[B] (f : (EnvT, SB) => B)
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, B] =
		IndexedReaderWriterStateErrorT {
			(env, sa) =>
				runE (env, sa) map {
					case (l, sb, _) =>
						(l, sb, f (env, sb))
					}
			}


	/**
	 * The inspectAskF method is similar to `inspectAsk`, with '''f''' being an
	 * effectual function. Errors within the produced ''F[B]'' are represented
	 * in the return value.
	 */
	def inspectAskF[B] (f : (EnvT, SB) => F[B])
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, B] =
		IndexedReaderWriterStateErrorT {
			(env, sa) =>
				runE (env, sa) flatMapF {
					case (l, sb, _) =>
						f (env, sb).map (b => (l, sb, b).asRight[(LogT, ErrorT)])
							.handleError {
								error =>
									(l -> error).asLeft[(LogT, SB, B)]
									}
					}
			}


	/**
	 * The inspectF method is similar to `inspect`, with '''f''' being an
	 * effectual function. Errors within the produced ''F[B]'' are represented
	 * in the return value.
	 */
	def inspectF[B] (f : SB => F[B])
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, B] =
		transformF ((l, sb, _) => f (sb) map (b => (l, sb, b)))


	/**
	 * The listen method combines ''A'' and ''LogT'' from '''this''' instance.
	 */
	def listen :
		IndexedReaderWriterStateErrorT[
			F,
			EnvT,
			LogT,
			ErrorT,
			SA,
			SB,
			(A, LogT)
			] =
		transform ((l, sb, a) => (l, sb, a -> l))


	/**
	 * The local method derives the initial ''EnvT'' from ''NewEnvT'' with the
	 * given functor '''f'''.
	 */
	def local[NewEnvT] (f : NewEnvT => EnvT)
		: IndexedReaderWriterStateErrorT[F, NewEnvT, LogT, ErrorT, SA, SB, A] =
		IndexedReaderWriterStateErrorT ((env, sa) => runE (f (env), sa))


	/**
	 * The map method modifies the result ''A'' in '''this''' instance.
	 */
	def map[B] (f : A => B)
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, B] =
		transform ((l, sb, a) => (l, sb, f (a)))


	/**
	 * The mapError method transforms ''ErrorT'' into ''NewErrorT'' if
	 * '''this''' instance represents an error condition.
	 */
	def mapError[NewErrorT] (f : ErrorT => NewErrorT)
		(
			implicit

			@implicitNotFound (
				"unable to resolve MonadError[${F}, ${NewErrorT}] for mapError"
				)
			monadErrorE0 : MonadError[F, NewErrorT]
		)
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, NewErrorT, SA, SB, A] =
		IndexedReaderWriterStateErrorT {
			runE (_, _).leftMap (_ map f) (monadErrorE0)
			}


	/**
	 * The mapK method transforms '''this''' context from ''F[_]'' to ''G[_]''.
	 */
	def mapK[G[_]] (f : F ~> G)
		(
			implicit

			@implicitNotFound (
				"unable to resolve MonadError[${G}, ${ErrorT}] for mapK"
				)
			monadErrorG : MonadError[G, ErrorT]
		)
		: IndexedReaderWriterStateErrorT[G, EnvT, LogT, ErrorT, SA, SB, A] =
		IndexedReaderWriterStateErrorT (runE (_, _) mapK f)


	/**
	 * The mapWritten transforms the ''LogT'' in '''this''' instance into
	 * '''NewLogT''' using the given functor '''f'''.
	 */
	def mapWritten[NewLogT] (f : LogT => NewLogT)
		: IndexedReaderWriterStateErrorT[F, EnvT, NewLogT, ErrorT, SA, SB, A] =
		transform (f) (identity ((_, _, _)))


	/**
	 * The modify method applies the given functor '''f''' to the state ''SB''.
	 */
	def modify[SC] (f : SB => SC)
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SC, A] =
		transform ((l, sb, a) => (l, f (sb), a))


	/**
	 * The modifyF method is similar to `modify`, with '''f''' being an
	 * effectual function. Errors within the produced ''F[B]'' are represented
	 * in the return value.
	 */
	def modifyF[SC] (f : SB => F[SC])
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SC, A] =
		transformF ((l, sb, a) => f (sb) map (sc => (l, sc, a)))


	/**
	 * The recoverWith method handles a subset of ''ErrorT'' instances by
	 * producing an '''IndexedReaderWriterStateErrorT''' based on the ''ErrorT''
	 * contained in '''this''' instance via the given '''pf''' functor.  If
	 * '''this''' is not in an error condition __or__ '''pf''' is not defined
	 * for the contained error, '''pf''' will not be evaluated.  Note that the
	 * `recover` method is __not__ defined in this type due to ''SA'' and ''SB''
	 * being potentially different types.
	 */
	def recoverWith (
		pf : PartialFunction[
			ErrorT,
			IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, A]
			]
		)
		(
			implicit

			@implicitNotFound (
				"unable to resolve Semigroup[${LogT}] for recoverWith"
				)
			semigroup : Semigroup[LogT]
		)
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, A] =
		IndexedReaderWriterStateErrorT {
			(env, sa) =>
				runE (env, sa) recoverWith {
					case (log, error) if pf isDefinedAt error =>
						pf (error).runE (env, sa)
							.bimap (
								le => (log |+| le._1) -> le._2,
								lsb => (log |+| lsb._1, lsb._2, lsb._3)
								)
					}
			}


	/**
	 * The reset method clears the ''LogT''.
	 */
	def reset (
		implicit

		@implicitNotFound ("unable to resolve Monoid[${LogT}] for reset")
		monoid : Monoid[LogT]
		)
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, A] =
		transform ((_, sb, a) => (monoid empty, sb, a))


	/**
	 * The run method evaluates the operations '''this''' instance represents
	 * using the given '''env''' and initial state '''sa''' instances.  Errors
	 * raised during evaluation are returned within
	 * ''F[(LogT, Either[ErrorT, (SB, A)])]''.  The ''LogT'' returned contains
	 * entries up to the first error encountered, if any.
	 */
	def run (env : EnvT, sa : SA) : F[(LogT, Either[ErrorT, (SB, A)])] =
		runE (env, sa).value map {
			_ fold (
				le => le._1 -> le._2.asLeft,
				lsa => lsa._1 -> (lsa._2, lsa._3).asRight
				)
			}


	/**
	 * The runA method provides syntactic convenience for invoking the `run`
	 * method and producing the ''A'' value of evaluating '''this''' when there
	 * are no errors.
	 */
	def runA (env : EnvT, sa : SA) : F[(LogT, Either[ErrorT, A])] =
		run (env, sa) map (_ map (_ map (_._2)))


	/**
	 * The runAndReport method `run`s '''this''' instance and then uses the
	 * given '''reporter''' to unconditionally emit the resultant ''LogT''.  The
	 * returned ''F[(SB, A)]'' therefore does not contain ''LogT''.  Any errors
	 * are represented within the returned ''F[_]'' context (which differs from
	 * what `run` returns).
	 */
	def runAndReport (env : EnvT, sa : SA)
		(reporter : (LogT, Either[ErrorT, (SB, A)]) => F[Unit])
		: F[(SB, A)] =
		run (env, sa).flatTap (reporter.tupled)
			.flatMap {
				case (_, either) =>
					either.liftTo[F]
				}


	/**
	 * The runAndReportA method provides syntactic convenience for invoking the
	 * `runAndReport` method and producing the ''A'' value of evaluating
	 * '''this''' when there are no errors.
	 */
	def runAndReportA (env : EnvT, sa : SA)
		(reporter : (LogT, Either[ErrorT, (SB, A)]) => F[Unit])
		: F[A] =
		runAndReport (env, sa) (reporter) map (_._2)


	/**
	 * The runAndReportS method provides syntactic convenience for invoking the
	 * `runAndReport` method and producing the ''SB'' state value of evaluating
	 * '''this''' when there are no errors.
	 */
	def runAndReportS (env : EnvT, sa : SA)
		(reporter : (LogT, Either[ErrorT, (SB, A)]) => F[Unit])
		: F[SB] =
		runAndReport (env, sa) (reporter) map (_._1)


	/**
	 * The runS method provides syntactic convenience for invoking the `run`
	 * method and producing the ''SB'' state value of evaluating '''this''' when
	 * there are no errors.
	 */
	def runS (env : EnvT, sa : SA) : F[(LogT, Either[ErrorT, SB])] =
		run (env, sa) map (_ map (_ map (_._1)))


	/**
	 * The set method replaces ''SB'' with the given '''state'''.
	 */
	def set[SC] (state : SC)
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SC, Unit] =
		transform ((l, _, _) => (l, state, ()))


	/**
	 * The setF method is similar to `set`, with the given '''state''' being an
	 * effectual value.
	 */
	def setF[SC] (state : F[SC])
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SC, Unit] =
		transformF ((l, _, _) => state map (sc => (l, sc, ())))


	/**
	 * The tell method combines the given ''LogT'' '''entries''' with '''this'''
	 * instance using the relevant [[cats.Semigroup]].
	 */
	def tell (entries : LogT)
		(
			implicit

			@implicitNotFound (
				"${LogT} must have a Semigroup defined and in the implicit scope"
				)
			semigroup : Semigroup[LogT]
		)
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, A] =
		transform ((l, sb, a) => (l |+| entries, sb, a))


	/**
	 * The tellF method is similar to `tell`, with '''f''' being an effectual
	 * function. Errors within the produced ''F[B]'' are represented in the
	 * return value.
	 */
	def tellF (entries : F[LogT])
		(
			implicit

			@implicitNotFound (
				"${LogT} must have a Semigroup defined and in the implicit scope"
				)
			semigroup : Semigroup[LogT]
		)
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, A] =
		transformF {
			(l, sb, a) =>
				entries map (ll => (l |+| ll, sb, a))
			}


	/**
	 * This version of the transform method provides the ability to change the
	 * type and/or content of ''LogT'', ''SB'', and ''A'' if '''this''' instance
	 * does __not__ represent an error condition.
	 */
	def transform[NewLogT, SC, B] (flog : LogT => NewLogT)
		(f : (NewLogT, SB, A) => (NewLogT, SC, B))
		: IndexedReaderWriterStateErrorT[F, EnvT, NewLogT, ErrorT, SA, SC, B] =
		IndexedReaderWriterStateErrorT {
			runE (_, _) transform {
				_ bimap (
					e => flog (e._1) -> e._2,
					lsba => f (flog (lsba._1), lsba._2, lsba._3)
					)
				}
			}


	/**
	 * This version of the transform method provides the ability to change the
	 * content of ''LogT'', type of ''SB'', and type of ''A'' if '''this'''
	 * instance does __not__ represent an error condition.
	 */
	def transform[SC, B] (f : (LogT, SB, A) => (LogT, SC, B))
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SC, B] =
		IndexedReaderWriterStateErrorT (runE (_, _) map f.tupled)


	/**
	 * The transformF method provides the ability to change the content of
	 * ''LogT'', type of ''SB'', and type of ''A'' using the effectual function
	 * '''f''' if '''this''' instance does __not__ represent an error condition.
	 * Errors within the produced ''F[_]'' context are represented in the return
	 * value.
	 */
	def transformF[SC, B] (f : (LogT, SB, A) => F[(LogT, SC, B)])
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SC, B] =
		IndexedReaderWriterStateErrorT {
			runE (_, _) flatMapF {
				case (log, sb, a) =>
					f (log, sb, a).map (_.asRight[(LogT, ErrorT)])
						.handleError {
							error =>
								(log -> error).asLeft[(LogT, SC, B)]
							}
				}
			}


	/**
	 * The written method retrieves the current ''LogT'' from '''this'''.
	 */
	def written
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, LogT] =
		transform ((l, sb, _) => (l, sb, l))
}


object IndexedReaderWriterStateErrorT
	extends IRWSETFunctions
		with IRWSETPrioritizedImplicits
{
	/// Self Types Constraints
	companion =>


	/// Class Types
	/**
	 * The '''Combinators''' `trait` defines methods which have a reduced
	 * generic parameter signature.
	 *
	 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.script]]
	 */
	sealed trait Combinators[F[_], EnvT, LogT, ErrorT]
	{
		/// Class Types
		final type StateType[SA, SB, A] = IndexedReaderWriterStateErrorT[
			F,
			EnvT,
			LogT,
			ErrorT,
			SA,
			SB,
			A
			]


		/// Instance Properties
		implicit protected def ME : MonadError[F, ErrorT]
		implicit protected def ML : Monoid[LogT]


		/**
		 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.ask]]
		 */
		@inline
		final def ask[S] : StateType[S, S, EnvT] = companion ask


		/**
		 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.get]]
		 */
		@inline
		final def get[S] : StateType[S, S, S] = companion get


		/**
		 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.inspect]]
		 */
		@inline
		final def inspect[S, A] (f : S => A) : StateType[S, S, A] =
			companion inspect f


		/**
		 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.inspectAsk]]
		 */
		@inline
		final def inspectAsk[S, A] (f : (EnvT, S) => A) : StateType[S, S, A] =
			companion inspectAsk f


		/**
		 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.inspectAskF]]
		 */
		@inline
		final def inspectAskF[S, A] (f : (EnvT, S) => F[A])
			: StateType[S, S, A] =
			companion inspectAskF f


		/**
		 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.inspectF]]
		 */
		@inline
		final def inspectF[S, A] (f : S => F[A]) : StateType[S, S, A] =
			companion inspectF f


		@inline
		final def liftF[S, A] (fa : F[A]) : StateType[S, S, A] =
			companion liftF fa


		/**
		 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.modify]]
		 */
		@inline
		final def modify[SA, SB] (f : SA => SB) : StateType[SA, SB, Unit] =
			companion modify f


		/**
		 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.modifyF]]
		 */
		@inline
		final def modifyF[SA, SB] (f : SA => F[SB]) : StateType[SA, SB, Unit] =
			companion modifyF f


		@inline
		final def pure[S, A] (a : A) : StateType[S, S, A] =
			companion pure a


		/**
		 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.set]]
		 */
		@inline
		final def set[S] (s : S) : StateType[S, S, Unit] = companion set s


		/**
		 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.setF]]
		 */
		@inline
		final def setF[S] (sb : F[S]) : StateType[S, S, Unit] =
			companion setF sb


		/**
		 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.tell]]
		 */
		@inline
		final def tell[S] (entries : LogT) : StateType[S, S, Unit] =
			companion tell entries


		/**
		 * @see [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.tellF]]
		 */
		@inline
		final def tellF[S] (entries : F[LogT]) : StateType[S, S, Unit] =
			companion tellF entries
	}


	final class PartiallyAppliedApplyF[ErrorT] (
		@unused
		private val dummy : Int = 0
		)
		extends AnyVal
	{
		/// Class Imports
		import cats.syntax.applicativeError._


		def apply[F[_], EnvT, LogT, SA, SB, A] (
			f : (EnvT, SA) => F[(LogT, SB, A)]
			)
			(
				implicit

				@implicitNotFound (
					"unable to resolve MonadError[${F}, ${ErrorT}] for apply"
					)
				monadError : MonadError[F, ErrorT],

				@implicitNotFound (
					"unable to resolve Monoid[${LogT}] for applyF"
					)
				monoid : Monoid[LogT]
			)
			: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, A] =
			new IndexedReaderWriterStateErrorT (
				(env, sa) =>
					EitherT (f (env, sa).attempt).leftMap (monoid.empty -> _)
				)
	}


	final class PartiallyAppliedScript[F[_], EnvT, LogT, ErrorT] ()
	{
		def apply[SA, SB, A] (
			block : Combinators[F, EnvT, LogT, ErrorT] =>
				IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, A]
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
			: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, A] =
			block (
				new Combinators[F, EnvT, LogT, ErrorT] {
					override protected val ME = monadError
					override protected val ML = monoid
					}
				)
	}


	/**
	 * The apply method provides support for functional-style
	 * '''IndexedReaderWriterStateErrorT''' creation.
	 */
	def apply[F[_], EnvT, LogT, ErrorT, SA, SB, A] (
		runE : (EnvT, SA) => EitherT[F, (LogT, ErrorT), (LogT, SB, A)]
		)
		(
			implicit

			@implicitNotFound (
				"unable to resolve MonadError[${F}, ${ErrorT}] for apply"
				)
			monadError : MonadError[F, ErrorT]
		)
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, A] =
		new IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, A] (
			runE
			)


	/**
	 * The applyF method employs the "partially applied" idiom to facilitate
	 * creating an
	 * [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT]]
	 * by only requiring collaborators to provide what type ''ErrorT'' is and
	 * allowing the compiler to derive the rest.
	 */
	def applyF[ErrorT] : PartiallyAppliedApplyF[ErrorT] =
		new PartiallyAppliedApplyF[ErrorT] ()


	/**
	 * The script method uses the "partially applied" idiom to support creating
	 * [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT]]
	 * definitions using those available in
	 * [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT.Combinators]].
	 * This technique allows definitions to avoid having to provide every
	 * parameter type needed for
	 * [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT]]
	 * in each method use.  For example:
	 *
	 * {{{
	 *     script[IO, MyEnv, Vector[LogEntry], Throwable] {
	 *         combinators =>
	 *             import combinators._
	 *
	 *             for {
	 *                 unsaved <- get[NewCompany]
	 *                 _ <- tell (Vector (LogEntry ("saving new company ...")))
	 *                 company <- inspectF[NewCompany, Company] (save)
	 *                 } yield (unsaved, company)
	 *         }
	 * }}}
	 */
	def script[F[_], EnvT, LogT, ErrorT]
		: PartiallyAppliedScript[F, EnvT, LogT, ErrorT] =
		new PartiallyAppliedScript[F, EnvT, LogT, ErrorT] ()
}


sealed trait IRWSETFunctions
{
	/// Class Imports
	import EitherT.rightT
	import cats.syntax.applicativeError._
	import cats.syntax.either._
	import cats.syntax.functor._


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
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, S, EnvT] =
		createPureInstance ((env, s) => (monoid.empty, s, env))


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
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, S, S] =
		createPureInstance ((_, s) => (monoid.empty, s, s))


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
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, S, A] =
		createPureInstance ((_, s) => (monoid.empty, s, f (s)))


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
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, S, A] =
		createPureInstance ((env, s) => (monoid.empty, s, f (env, s)))


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
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, S, A] =
		createRecoverableInstance {
			(env, s) =>
				f (env, s) map ((monoid.empty, s, _))
			}


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
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, S, A] =
		createRecoverableInstance {
			(_, sa) =>
				f (sa) map ((monoid.empty, sa, _))
			}


	/**
	 * The liftF method attempts to incorporate the given '''fa''' instance as
	 * the result of a new
	 * [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT]]
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
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, S, A] =
		createRecoverableInstance ((_, s) => fa map ((monoid.empty, s, _)))


	final def modify[F[_], EnvT, LogT, ErrorT, SA, SB] (f : SA => SB)
		(
			implicit

			@implicitNotFound (
				"unable to resolve MonadError[${F}, ${ErrorT}] for modify"
				)
			monadError : MonadError[F, ErrorT],

			@implicitNotFound ("unable to resolve Monoid[${LogT}] for modify")
			monoid : Monoid[LogT]
		)
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, Unit] =
		createPureInstance ((_, sa) => (monoid.empty, f (sa), {}))


	final def modifyF[F[_], EnvT, LogT, ErrorT, SA, SB] (f : SA => F[SB])
		(
			implicit

			@implicitNotFound (
				"unable to resolve MonadError[${F}, ${ErrorT}] for modifyF"
				)
			monadError : MonadError[F, ErrorT],

			@implicitNotFound ("unable to resolve Monoid[${LogT}] for modifyF")
			monoid : Monoid[LogT]
		)
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, Unit] =
		createRecoverableInstance {
			(_, sa) =>
				f (sa) map ((monoid.empty, _, {}))
			}


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
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, S, A] =
		createPureInstance ((_, s) => (monoid.empty, s, a))


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
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, S, Unit] =
		createPureInstance ((_, _) => (monoid.empty, s, {}))


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
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, S, Unit] =
		createRecoverableInstance ((_, _) => s map ((monoid.empty, _, {})))


	final def tell[F[_], EnvT, LogT, ErrorT, S] (entries : LogT)
		(
			implicit

			@implicitNotFound (
				"unable to resolve MonadError[${F}, ${ErrorT}] for tell"
				)
			monadError : MonadError[F, ErrorT]
		)
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, S, Unit] =
		createPureInstance ((_, s) => (entries, s, {}))


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
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, S, Unit] =
		createRecoverableInstance ((_, s) => entries map ((_, s, {})))


	@inline
	private def createPureInstance[F[_], EnvT, LogT, ErrorT, SA, SB, A] (
		f : (EnvT, SA) => (LogT, SB, A)
		)
		(implicit monadError : MonadError[F, ErrorT])
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, A] =
		IndexedReaderWriterStateErrorT ((env, sa) => rightT (f (env, sa)))


	@inline
	private def createRecoverableInstance[F[_], EnvT, LogT, ErrorT, SA, SB, A] (
		f : (EnvT, SA) => F[(LogT, SB, A)]
		)
		(
			implicit

			monadError : MonadError[F, ErrorT],
			monoid : Monoid[LogT]
		)
		: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, A] =
		IndexedReaderWriterStateErrorT {
			(env, sa) =>
				EitherT (
					f (env, sa).map (_.asRight[(LogT, ErrorT)])
						.handleError {
							error =>
								(monoid.empty -> error).asLeft[(LogT, SB, A)]
							}
					)
			}
}


/**
 * The '''IRWSETPrioritizedImplicits''' type defines prioritized `implicit`
 * Cats type class instances available for
 * [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT]].
 * Below are the priorities in descending order:
 *
 *   1. Type classes which are the most specialized in their hierarchy.
 *
 *   1. Type classes requiring ''SA'' and ''SB'' to be the same.
 *
 *   1. Type classes supporting differing ''SA'' and ''SB'' and are
 *       generalizations of higher-priority ones.
 */
sealed trait IRWSETPrioritizedImplicits
	extends IRWSETPrioritizedImplicits.MonoState
{
	/// Implicit Conversions
	implicit final def irwsetBifunctor[F[_], EnvT, LogT, ErrorT, SA]
		: Bifunctor[
			IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, *, *]
			] =
		new Bifunctor[
			IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, *, *]
			] {
			override def bimap[A, B, C, D] (
				fab : IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, A, B]
				)
				(f : A => C, g : B => D)
				: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, C, D] =
				fab.bimap (f, g)
			}


	implicit final def irwsetContravariant[F[_], EnvT, LogT, ErrorT, SB, R]
		: Contravariant[
			IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, *, SB, R]
			] =
		new Contravariant[
			IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, *, SB, R]
			] {
			override def contramap[A, B] (
				fa : IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, A, SB, R]
				)
				(f : B => A)
				: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, B, SB, R] =
				fa contramap f
			}
}


private[chassis] object IRWSETPrioritizedImplicits
{
	/// Class Types
	private sealed trait AbstractFunctor[F[_], EnvT, LogT, ErrorT, SA, SB]
		extends Functor[
			IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, *]
			]
	{
		final override def map[A, B] (
			fa : IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, A]
			)
			(f : A => B)
			: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, B] =
			fa map f
	}


	private final class DefaultMonadError[F[_], EnvT, LogT, ErrorT, S] ()
		(
			implicit

			private val monadError : MonadError[F, ErrorT],
			private val monoid : Monoid[LogT]
		)
		extends MonadError[
			ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, *],
			ErrorT
			]
			with AbstractFunctor[F, EnvT, LogT, ErrorT, S, S]
	{
		/// Class Imports
		import cats.syntax.either._
		import cats.syntax.semigroup._


		/// Instance Properties
		private val indexed = IndexedReaderWriterStateErrorT


		override def flatMap[A, B] (
			fa : ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, A]
			)
			(f : A => ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, B])
			: ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, B] =
			fa flatMap f


		override def handleErrorWith[A] (
			fa : ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, A]
			)
			(f : ErrorT => ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, A])
			: ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, A] =
			fa handleErrorWith f


		override def pure[A] (a : A)
			: ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, A] =
			indexed pure a


		override def raiseError[A] (error : ErrorT)
			: ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, A] =
			indexed liftF (monadError raiseError error)


		override def recoverWith[A] (
			fa : ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, A]
			)
			(
				pf :
					PartialFunction[
						ErrorT,
						ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, A]
						]
			)
			: ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, A] =
			fa recoverWith pf


		override def tailRecM[A, B] (a : A)
			(
				f : A =>
					ReaderWriterStateErrorT[
						F,
						EnvT,
						LogT,
						ErrorT,
						S,
						Either[A, B]
						]
			)
			: ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, B] =
			IndexedReaderWriterStateErrorT {
				(env, s0) =>
					val result = monadError.tailRecM ((monoid.empty, s0, a)) {
						current =>
							val next = f (current._3).runE (env, current._2)
								.value

							monadError.map (next) {
								/// Continue with the recursion.
								case Right ((l, s, Left (a))) =>
									(current._1 |+| l, s, a).asLeft

								/// Stop recursion due to an error.
								case Left ((l, e)) =>
									Left ((current._1 |+| l, e)).asRight

								/// Stop recursion due to a terminating
								/// successful condition.
								case Right ((l, s, Right (a))) =>
									Right ((current._1 |+| l, s, a)).asRight
								}
						}

					EitherT (result)
				}
	}


	sealed trait IndexedState
	{
		/// Implicit Conversions
		implicit final def irwsetFunctor[F[_], EnvT, LogT, ErrorT, SA, SB]
			: Functor[
			IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, *]
			] =
			new Functor[
				IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, *]
				] {
				override def map[A, B] (
					fa : IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, A]
					)
					(f : A => B)
					: IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, SA, SB, B] =
					fa map f
			}
	}


	sealed trait MonoState
		extends IndexedState
	{
		/// Implicit Conversions
		implicit final def rwsetMonadError[F[_], EnvT, LogT, ErrorT, S] (
			implicit

			monadError : MonadError[F, ErrorT],
			monoid : Monoid[LogT]
			)
			: MonadError[
				ReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, *],
				ErrorT
				] =
			new DefaultMonadError ()
	}
}

