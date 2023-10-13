package com.github.osxhacker.demo.chassis.effect

import scala.concurrent.{
	ExecutionContext,
	Future
	}

import scala.language.postfixOps
import scala.util.{
	Success,
	Try
	}

import cats._
import cats.effect.IO

import com.github.osxhacker.demo.chassis.domain.ErrorOr


/**
 * The '''Pointcut''' type is a model of the TYPE CLASS pattern and provides the
 * ability to do limited functional
 * [[https://en.wikipedia.org/wiki/Aspect-oriented_programming Aspect Oriented Programming]]
 * natively in Scala.
 *
 * ==General Form==
 *
 * ===Entering Functors===
 *
 * {{{
 *     type FunctorVersion = () =&gt; Unit
 *     type ContainerVersion = () =&gt; F[Unit]
 * }}}
 *
 * The '''entering''' functors are evaluated __before__ there exists a
 * ''ResultT'' and, therefore, they produce ''Unit'', within or without
 * ''F[_]''.  The `FunctorVersion` can be used in a referentially transparent
 * manner when used in conjunction with other '''Pointcut''' methods to create
 * a closure.
 *
 * ===Leaving Functors===
 *
 * {{{
 *     type FunctorVersion = Endo[ResultT]
 *     type ContainerVersion = ResultT =&gt; F[ResultT]
 * }}}
 *
 * The '''leaving''' functors are evaluated during "happy path" execution and
 * have the ability to modify the return value if desired.  Effects performed
 * within ''F[_]'' in the `ContainerVersion` are supported and fully expected.
 *
 * ===OnError Functors===
 *
 * {{{
 *     type Signature = Throwable =&gt; F[Unit]
 * }}}
 *
 * The '''onError''' functors have a return type of ''F[Unit]'' in order to
 * support error reporting effects.  Since the container ''F[_]'' is in an
 * error state when '''onError''' functors are evaluated, recovering from them
 * is out of scope for '''Pointcut''' logic.
 *
 * @see [[com.github.osxhacker.demo.chassis.effect.Advice]]
 */
trait Pointcut[F[_]]
{
	/// Class Imports
	import mouse.any._


	/**
	 * The after method ensures that '''leaving''' always has the opportunity to
	 * manipulate the ''ResultT'' iff '''efa''' is successful.
	 */
	def after[ResultT] (efa : Eval[F[ResultT]])
		(leaving : Endo[ResultT])
		: Eval[F[ResultT]]


	/**
	 * The afterF method ensures that '''leaving''' has the opportunity to
	 * manipulate ''F[ResultT]'' __after__ it has been constructed iff '''efa'''
	 * is successful.
	 */
	def afterF[ResultT] (efa : Eval[F[ResultT]])
		(leaving : ResultT => F[ResultT])
		: Eval[F[ResultT]]


	/**
	 * The always method ensures '''leaving''' is invoked when '''efa''' is
	 * successful and '''onError''' when not.
	 */
	def always[ResultT] (efa : Eval[F[ResultT]])
		(leaving : Endo[ResultT], onError : Throwable => F[Unit])
		: Eval[F[ResultT]]


	/**
	 * The alwaysF method ensures '''leaving''' is invoked when '''efa''' is
	 * successful and '''onError''' when not.  Both must produce their results
	 * within the context of ''F''.
	 */
	def alwaysF[ResultT] (efa : Eval[F[ResultT]])
		(leaving : ResultT => F[ResultT], onError : Throwable => F[Unit])
		: Eval[F[ResultT]]


	/**
	 * The around method allows logic to be invoked when '''entering''',
	 * '''leaving''', and when '''efa''' encounters an error.  Conceptually, it
	 * is similar to:
	 *
	 * {{{
	 *    entering ().andThen (_ =&gt; fa)
	 *        .andThen (leaving)
	 *        .handleErrorWith (ex =&gt; onError (ex))
	 * }}}
	 */
	def around[ResultT] (efa : Eval[F[ResultT]])
		(
			entering : () => Unit,
			leaving : Endo[ResultT],
			onError : Throwable => F[Unit]
		)
		: Eval[F[ResultT]] =
		before (efa) (entering) |> (always (_) (leaving, onError))


	/**
	 * The aroundF method allows logic to be invoked when '''entering''',
	 * '''leaving''', and when '''efa''' encounters an error.  Conceptually, it
	 * is similar to:
	 *
	 * {{{
	 *    entering ().flatMap (_ =gt; fa)
	 *        .flatMap (leaving)
	 *        .handleErrorWith (ex =&gt; onError (ex))
	 * }}}
	 */
	def aroundF[ResultT] (efa : Eval[F[ResultT]])
		(
			entering : () => F[Unit],
			leaving : ResultT => F[ResultT],
			onError : Throwable => F[Unit]
		)
		: Eval[F[ResultT]] =
		beforeF (efa) (entering) |> (alwaysF (_) (leaving, onError))


	/**
	 * The before method ensures '''entering''' is invoked before '''efa''' is
	 * evaluated.
	 */
	def before[ResultT] (efa : Eval[F[ResultT]])
		(entering : () => Unit)
		: Eval[F[ResultT]]


	/**
	 * The beforeF method ensures '''Pointcut''' logic has the opportunity to
	 * manipulate '''efa''' __before__ it has been constructed.
	 */
	def beforeF[ResultT] (efa : Eval[F[ResultT]])
		(entering : () => F[Unit])
		: Eval[F[ResultT]]


	/**
	 * The bracket method allows '''Pointcut''' logic to manage a ''ResourceT''
	 * related to '''efa'''.  The '''acquire''' functor must be invoked before
	 * ''F[ResultT]'' is evaluated and '''release''' must be invoked
	 * unconditionally afterward.
	 */
	def bracket[ResultT, ResourceT] (efa : Eval[F[ResultT]])
		(acquire : () => ErrorOr[ResourceT])
		(release : ResourceT => ErrorOr[ResultT] => Unit)
		: Eval[F[ResultT]]


	/**
	 * The finalizeWith method is similar to `always` in that the given
	 * '''finalizer''' is always evaluated after '''efa''' is computed, no
	 * matter if '''efa''' succeeds or fails.
	 */
	def finalizeWith[ResultT] (efa : Eval[F[ResultT]])
		(finalizer : () => F[Unit])
		: Eval[F[ResultT]]
}


object Pointcut
{
	/// Class Types
	/**
	 * The '''IOPointcut''' `object` defines the
	 * [[com.github.osxhacker.demo.chassis.effect.Pointcut]] contract for the
	 * [[cats.effect.IO]] [[cats.Monad]].  Of note is that __all__ operations
	 * are evaluated in the [[cats.Now]] [[cats.Eval]] container since program
	 * evaluation __must__ be defined within [[cats.effect.IO]].
	 *
	 * Another thing to note about [[cats.effect.IO]] is that the implementation
	 * will cancel fibers during normal operations as well as when explicitly
	 * asked to do so.  This is why cancellations are not treated as errors
	 * here.
	 */
	implicit object IOPointcut
		extends Pointcut[IO]
	{
		/// Class Imports
		import cats.syntax.monadError._


		override def after[ResultT] (efa : Eval[IO[ResultT]])
			(leaving : Endo[ResultT])
			: Eval[IO[ResultT]] =
			efa map (_ map leaving)


		override def afterF[ResultT] (efa : Eval[IO[ResultT]])
			(leaving : ResultT => IO[ResultT])
			: Eval[IO[ResultT]] =
			efa map (_ flatMap leaving)


		override def always[ResultT] (efa : Eval[IO[ResultT]])
			(
				leaving : Endo[ResultT],
				onError : Throwable => IO[Unit]
			)
			: Eval[IO[ResultT]] =
			efa map {
				_.map (leaving)
					.onError (onError)
				}


		override def alwaysF[ResultT] (efa : Eval[IO[ResultT]])
			(
				leaving : ResultT => IO[ResultT],
				onError : Throwable => IO[Unit]
			)
			: Eval[IO[ResultT]] =
			efa map {
				_.flatMap (leaving)
					.onError (onError)
				}


		override def around[ResultT] (efa : Eval[IO[ResultT]])
			(
				entering : () => Unit,
				leaving : Endo[ResultT],
				onError : Throwable => IO[Unit]
			)
			: Eval[IO[ResultT]] =
			Now (IO delay entering ()) flatMap {
				prior =>
					always (efa map (prior >> _)) (leaving, onError)
				}


		override def aroundF[ResultT] (efa : Eval[IO[ResultT]])
			(
				entering : () => IO[Unit],
				leaving : ResultT => IO[ResultT],
				onError : Throwable => IO[Unit]
			)
			: Eval[IO[ResultT]] =
			Now (IO defer entering ()).flatMap {
				prior =>
					efa map {
						fa =>
							(prior >> fa.flatMap (leaving)).onError (onError)
						}
				}


		override def before[ResultT] (efa : Eval[IO[ResultT]])
			(entering : () => Unit)
			: Eval[IO[ResultT]] =
			Now (IO delay entering ()) flatMap {
				prior =>
					efa map (prior >> _)
				}


		override def beforeF[ResultT] (efa : Eval[IO[ResultT]])
			(entering : () => IO[Unit])
			: Eval[IO[ResultT]] =
			Now (IO defer entering ()) flatMap {
				prior =>
					efa map (prior >> _)
				}


		override def bracket[ResultT, ResourceT] (efa : Eval[IO[ResultT]])
			(acquire : () => ErrorOr[ResourceT])
			(release : ResourceT => ErrorOr[ResultT] => Unit)
			: Eval[IO[ResultT]] =
			efa map {
				inner =>
					IO.uncancelable {
						poll =>
							IO.fromEither (acquire ())
								.flatMap {
									resource =>
										IO.defer (poll (inner))
											.start
											.flatTap (_ => IO.cede)
											.flatMap (_.join)
											.flatMap (_.embedError)
											.attemptTap {
												result =>
													IO (release (resource) (result))
												}
									}
						}
				}


		override def finalizeWith[ResultT] (efa : Eval[IO[ResultT]])
			(finalizer : () => IO[Unit])
			: Eval[IO[ResultT]] =
			efa map (_ guarantee finalizer ())
	}


	implicit object TryPointcut
		extends Pointcut[Try]
	{
		/// Class Imports
		import cats.syntax.all._


		override def after[ResultT] (efa : Eval[Try[ResultT]])
			(leaving : Endo[ResultT])
			: Eval[Try[ResultT]] =
			efa map (_ map leaving)


		override def afterF[ResultT] (efa : Eval[Try[ResultT]])
			(leaving : ResultT => Try[ResultT])
			: Eval[Try[ResultT]] =
			efa map (_ flatMap leaving)


		override def always[ResultT] (efa : Eval[Try[ResultT]])
			(leaving : Endo[ResultT], onError : Throwable => Try[Unit])
			: Eval[Try[ResultT]] =
			efa map {
				_.map (leaving)
					.onError (onError (_))
				}


		override def alwaysF[ResultT] (efa : Eval[Try[ResultT]])
			(
				leaving : ResultT => Try[ResultT],
				onError : Throwable => Try[Unit]
			)
			: Eval[Try[ResultT]] =
			efa map {
				_.flatMap (leaving)
					.onError (onError (_))
				}


		override def before[ResultT] (efa : Eval[Try[ResultT]])
			(entering : () => Unit)
			: Eval[Try[ResultT]] =
			new Later (entering) flatMap (_ => efa)


		override def beforeF[ResultT] (efa : Eval[Try[ResultT]])
			(entering : () => Try[Unit])
			: Eval[Try[ResultT]] =
			new Later (entering) flatMap {
				prior =>
					efa map (prior >> _)
				}


		override def bracket[ResultT, ResourceT] (efa : Eval[Try[ResultT]])
			(acquire : () => ErrorOr[ResourceT])
			(release : ResourceT => ErrorOr[ResultT] => Unit)
			: Eval[Try[ResultT]] =
			Later (
				acquire ().toTry
					.flatMap {
						resource =>
							efa.value
								.attemptTap {
									result =>
										release (resource) (result)
										Success ({})
									}
						}
				)


		override def finalizeWith[ResultT] (efa : Eval[Try[ResultT]])
			(finalizer : () => Try[Unit])
			: Eval[Try[ResultT]] =
			efa map {
				_ attemptTap (_ => finalizer ())
				}
	}


	/// Implicit Conversions
	implicit def futurePointcut (implicit ec : ExecutionContext)
		: Pointcut[Future] =
		new Pointcut[Future] {
			/// Class Imports
			import cats.syntax.all._


			override def after[ResultT] (efa : Eval[Future[ResultT]])
				(leaving : Endo[ResultT])
				: Eval[Future[ResultT]] =
				efa map (_ map leaving)


			override def afterF[ResultT] (efa : Eval[Future[ResultT]])
				(leaving : ResultT => Future[ResultT])
				: Eval[Future[ResultT]] =
				efa map (_ flatMap leaving)


			override def always[ResultT] (efa : Eval[Future[ResultT]])
				(leaving : Endo[ResultT], onError : Throwable => Future[Unit])
				: Eval[Future[ResultT]] =
				efa map {
					_.map (leaving)
						.onError (onError (_))
					}


			override def alwaysF[ResultT] (efa : Eval[Future[ResultT]])
				(
					leaving : ResultT => Future[ResultT],
					onError : Throwable => Future[Unit]
				)
				: Eval[Future[ResultT]] =
				efa map {
					_.flatMap (leaving)
						.onError (onError (_))
					}


			override def before[ResultT] (efa : Eval[Future[ResultT]])
				(entering : () => Unit)
				: Eval[Future[ResultT]] =
				Later (Future (entering ())) flatMap (_ => efa)


			override def beforeF[ResultT] (efa : Eval[Future[ResultT]])
				(entering : () => Future[Unit])
				: Eval[Future[ResultT]] =
				new Later (entering) flatMap {
					prior =>
						efa map (prior >> _)
					}


			override def bracket[ResultT, ResourceT] (
				efa : Eval[Future[ResultT]]
				)
				(acquire : () => ErrorOr[ResourceT])
				(release : ResourceT => ErrorOr[ResultT] => Unit)
				: Eval[Future[ResultT]] =
				Later {
					Future.fromTry (acquire ().toTry)
						.flatMap {
							resource =>
								efa.value
									.attemptTap {
										result =>
											release (resource) (result)
											Future.unit
										}
							}
					}


			override def finalizeWith[ResultT] (efa : Eval[Future[ResultT]])
				(finalizer : () => Future[Unit])
				: Eval[Future[ResultT]] =
				efa map {
					_ attemptTap (_ => finalizer ())
				}
		}


	/**
	 * The apply method is a summoner which resolves an '''Pointcut''' from the
	 * `implicit` scope.
	 */
	def apply[F[_]] ()
		(implicit instance : Pointcut[F])
		: Pointcut[F] =
		instance
}

