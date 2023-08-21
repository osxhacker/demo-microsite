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
 * ability to do limited
 * [[https://en.wikipedia.org/wiki/Aspect-oriented_programming Aspect Oriented Programming]]
 * natively in Scala.
 *
 * @see [[com.github.osxhacker.demo.chassis.effect.Advice]]
 */
trait Pointcut[F[_]]
{
	/// Class Imports
	import Pointcut.noop
	import mouse.any._


	/**
	 * The after method ensures that '''leaving''' is always invoked when
	 * '''efa''' is successful.
	 */
	def after[ResultT] (efa : Eval[F[ResultT]])
		(leaving : ResultT => Unit)
		: Eval[F[ResultT]]


	/**
	 * The afterF method ensures '''Pointcut''' logic has the opportunity to
	 * manipulate '''efa''' __after__ it has been constructed.  There is no
	 * `aroundF` equivalent as all it could do is introduce logic then
	 * immediately ignore its result.
	 */
	def afterF[ResultT] (efa : Eval[F[ResultT]])
		(leaving : Endo[F[ResultT]])
		: Eval[F[ResultT]]


	/**
	 * The always method ensures '''leaving''' is invoked when '''efa''' is
	 * successful and '''onError''' when not.
	 */
	def always[ResultT] (efa : Eval[F[ResultT]])
		(leaving : ResultT => Unit, onError : Throwable => Unit)
		: Eval[F[ResultT]]


	/**
	 * The alwaysF method ensures '''leaving''' is invoked when '''efa''' is
	 * successful and '''onError''' when not.  Both must produce their results
	 * within the context of ''F''.
	 */
	def alwaysF[ResultT] (efa : Eval[F[ResultT]])
		(leaving : ResultT => F[Unit], onError : Throwable => F[Unit])
		: Eval[F[ResultT]]


	/**
	 * The around method allows logic to be invoked when '''entering''',
	 * '''leaving''', and when '''efa''' encounters an error.  Conceptually, it
	 * is similar to:
	 *
	 * {{{
	 *    entering ().andThen (efa)
	 *        .andThen (r =&gt; leaving (r))
	 *        .handleError (ex =&gt; onError (ex))
	 * }}}
	 *
	 * There is no `aroundF` equivalent as all it could do is introduce logic
	 * then immediately ignore its result.
	 */
	def around[ResultT] (efa : Eval[F[ResultT]])
		(
			entering : () => Unit,
			leaving : ResultT => Unit,
			onError : Throwable => Unit = noop
		)
		: Eval[F[ResultT]] =
		before (efa) (entering) |> (always (_) (leaving, onError))


	/**
	 * The before method ensures '''entering''' is invoked before '''efa''' is
	 * evaluated.
	 */
	def before[ResultT] (efa : Eval[F[ResultT]])
		(entering : () => Unit)
		: Eval[F[ResultT]]


	/**
	 * The beforeF method ensures '''Pointcut''' logic has the opportunity to
	 * manipulate '''efa''' __before__ it has been constructed.  There is no
	 * `aroundF` equivalent as all it could do is introduce logic then
	 * immediately remove it.
	 */
	def beforeF[ResultT] (efa : Eval[F[ResultT]])
		(entering : () => F[ResultT])
		: Eval[F[ResultT]]


	/**
	 * The bracket method allows '''Pointcut''' logic to manage a ''ResourceT''
	 * related to '''efa'''.  The '''acquire''' functor must be invoked before
	 * ''F[ResultT]'' is evaluated and '''release''' must be invoked
	 * unconditionally afterward.
	 */
	def bracket[ResultT, ResourceT] (efa : Eval[F[ResultT]])
		(acquire : () => ResourceT)
		(release : ResourceT => ErrorOr[ResultT] => Unit)
		: Eval[F[ResultT]]


	/**
	 * The finalizeWith method is similar to `always` in that the given
	 * '''finalizer''' is always evaluated after '''efa''' is computed, no
	 * matter if '''efa''' succeeds, fails, or is cancelled.
	 */
	def finalizeWith[ResultT] (efa : Eval[F[ResultT]])
		(finalizer : () => Unit)
		: Eval[F[ResultT]] =
		always (efa) (_ => finalizer (), _ => finalizer ())
}


object Pointcut
{
	/// Class Types
	implicit object IOPointcut
		extends Pointcut[IO]
	{
		/// Class Imports
		import cats.syntax.monadError._


		override def after[ResultT] (efa : Eval[IO[ResultT]])
			(leaving : ResultT => Unit)
			: Eval[IO[ResultT]] =
			efa map (_.flatTap (r => IO (leaving (r))))


		override def afterF[ResultT] (efa : Eval[IO[ResultT]])
			(leaving : Endo[IO[ResultT]])
			: Eval[IO[ResultT]] =
			efa map (r => r <* leaving (r))


		override def always[ResultT] (efa : Eval[IO[ResultT]])
			(leaving : ResultT => Unit, onError : Throwable => Unit)
			: Eval[IO[ResultT]] =
			efa map {
				_.guaranteeCase {
					_.fold (
						canceled = IO.unit,
						errored = ex => IO (onError (ex)),
						completed = _ map leaving
						)
					}
				}


		override def alwaysF[ResultT] (efa : Eval[IO[ResultT]])
			(leaving : ResultT => IO[Unit], onError : Throwable => IO[Unit])
			: Eval[IO[ResultT]] =
			efa map {
				_.guaranteeCase {
					_.fold (
						canceled = IO.unit,
						errored = onError,
						completed = _ flatMap leaving
						)
					}
				}


		override def around[ResultT] (efa : Eval[IO[ResultT]])
			(
				entering : () => Unit,
				leaving : ResultT => Unit,
				onError : Throwable => Unit = noop
			)
			: Eval[IO[ResultT]] =
			new Later (entering) flatMap (_ => always (efa) (leaving, onError))


		override def before[ResultT] (efa : Eval[IO[ResultT]])
			(entering : () => Unit)
			: Eval[IO[ResultT]] =
			new Later (entering) flatMap (_ => efa)


		override def beforeF[ResultT] (efa : Eval[IO[ResultT]])
			(entering : () => IO[ResultT])
			: Eval[IO[ResultT]] =
			new Later (entering) flatMap {
				prior =>
					efa map (prior >> _)
				}


		override def bracket[ResultT, ResourceT] (efa : Eval[IO[ResultT]])
			(acquire : () => ResourceT)
			(release : ResourceT => ErrorOr[ResultT] => Unit)
			: Eval[IO[ResultT]] =
			Later (
				IO.uncancelable {
					poll =>
						IO (acquire ()).flatMap {
							resource =>
								IO.defer (poll (efa.value))
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
				)


		override def finalizeWith[ResultT] (efa : Eval[IO[ResultT]])
			(finalizer : () => Unit)
			: Eval[IO[ResultT]] =
			efa map (_.guarantee (IO (finalizer ())))
	}


	implicit object TryPointcut
		extends Pointcut[Try]
	{
		/// Class Imports
		import cats.syntax.all._


		override def after[ResultT] (fa : Eval[Try[ResultT]])
			(leaving : ResultT => Unit)
			: Eval[Try[ResultT]] =
			fa map (_.flatTap (leaving (_).pure[Try]))


		override def afterF[ResultT] (fa : Eval[Try[ResultT]])
			(leaving : Endo[Try[ResultT]])
			: Eval[Try[ResultT]] =
			fa map (r => r <* leaving (r))


		override def always[ResultT] (fa : Eval[Try[ResultT]])
			(leaving : ResultT => Unit, onError : Throwable => Unit)
			: Eval[Try[ResultT]] =
			fa map {
				_.attemptTap {
					_.fold (
						 ex => onError (ex).pure[Try] *> ex.raiseError[Try, ResultT],
						_.pure[Try].flatTap (leaving (_).pure[Try])
						)
					}
				}


		override def alwaysF[ResultT] (fa : Eval[Try[ResultT]])
			(leaving : ResultT => Try[Unit], onError: Throwable => Try[Unit])
			: Eval[Try[ResultT]] =
			fa map {
				_.attemptTap {
					_.fold (
						ex => onError (ex) *> ex.raiseError[Try, ResultT],
						_.pure[Try].flatTap (leaving)
						)
					}
				}


		override def before[ResultT] (fa : Eval[Try[ResultT]])
			(entering : () => Unit)
			: Eval[Try[ResultT]] =
			new Later (entering) flatMap (_ => fa)


		override def beforeF[ResultT] (fa : Eval[Try[ResultT]])
			(entering : () => Try[ResultT])
			: Eval[Try[ResultT]] =
			new Later (entering) flatMap {
				prior =>
					fa map (prior >> _)
				}


		override def bracket[ResultT, ResourceT] (efa : Eval[Try[ResultT]])
			(acquire : () => ResourceT)
			(release : ResourceT => ErrorOr[ResultT] => Unit)
			: Eval[Try[ResultT]] =
			Later (
				Try (acquire ()).flatMap {
					resource =>
						efa.value
							.attemptTap {
								result =>
									release (resource) (result)
									Success ({})
								}
					}
				)
	}


	/// Implicit Conversions
	implicit def futurePointcut (implicit ec : ExecutionContext)
		: Pointcut[Future] =
		new Pointcut[Future] {
			/// Class Imports
			import cats.syntax.all._


			override def after[ResultT] (efa : Eval[Future[ResultT]])
				(leaving : ResultT => Unit)
				: Eval[Future[ResultT]] =
				efa map (_.flatTap (r => Future (leaving (r))))


			override def afterF[ResultT] (efa : Eval[Future[ResultT]])
				(leaving : Endo[Future[ResultT]])
				: Eval[Future[ResultT]] =
				efa map (leaving (_))


			override def always[ResultT] (efa : Eval[Future[ResultT]])
				(leaving : ResultT => Unit, onError : Throwable => Unit)
				: Eval[Future[ResultT]] =
				after (efa) (leaving) map {
					_.onError (ex => Future (onError (ex)))
					}


			override def alwaysF[ResultT] (efa : Eval[Future[ResultT]])
				(
					leaving : ResultT => Future[Unit],
					onError : Throwable => Future[Unit]
				)
				: Eval[Future[ResultT]] =
				efa map {
					_.flatTap (leaving)
						.onError (onError (_))
					}


			override def before[ResultT] (efa : Eval[Future[ResultT]])
				(entering : () => Unit)
				: Eval[Future[ResultT]] =
				Later (Future (entering ())) flatMap (_ => efa)


			override def beforeF[ResultT] (efa : Eval[Future[ResultT]])
				(entering : () => Future[ResultT])
				: Eval[Future[ResultT]] =
				new Later (entering) flatMap (_ => efa)


			override def bracket[ResultT, ResourceT] (
				efa : Eval[Future[ResultT]]
				)
				(acquire : () => ResourceT)
				(release : ResourceT => ErrorOr[ResultT] => Unit)
				: Eval[Future[ResultT]] =
				Later {
					Future (acquire ()).flatMap {
						resource =>
							efa.value
								.attemptTap {
									result =>
										release (resource) (result)
										Future.unit
									}
						}
					}
		}


	/// Instance Properties
	private[effect] val noop : Throwable => Unit = _ => {}


	/**
	 * The apply method is a summoner which resolves an '''Pointcut''' from the
	 * `implicit` scope.
	 */
	def apply[F[_]] ()
		(implicit instance : Pointcut[F])
		: Pointcut[F] =
		instance
}

