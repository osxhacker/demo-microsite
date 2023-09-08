package com.github.osxhacker.demo.chassis.effect

import scala.concurrent.Future
import scala.language.postfixOps

import cats.{
	Eval,
	Later,
	MonadThrow,
	Now
	}

import org.scalatest.{
	Assertion,
	AsyncTestSuite
	}

import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AsyncWordSpecLike


/**
 * The '''PointcutBehaviours''' type defines the unit tests which certify the
 * [[com.github.osxhacker.demo.chassis.effect.Pointcut]] type for fitness of
 * purpose and serves as an exemplar of its generic use.  Its name is inspired
 * by the
 * [[https://www.scalatest.org/user_guide/sharing_tests ScalaTest sharing documentation]].
 */
trait PointcutBehaviours
	extends AsyncWordSpecLike
		with Diagrams
{
	/// Self Type Constraints
	this : AsyncTestSuite =>


	/// Class Imports
	import PointcutBehaviours._
	import cats.syntax.applicative._
	import cats.syntax.applicativeError._
	import cats.syntax.either._
	import cats.syntax.functor._
	import mouse.int._


	/**
	 * The functionalAOP method is the entry point for exercising __all__
	 * [[com.github.osxhacker.demo.chassis.effect.Pointcut]] methods.  Each is
	 * delegated to a specific `private` method named after the
	 * [[com.github.osxhacker.demo.chassis.effect.Pointcut]] method it verifies.
	 */
	protected def functionalAOP[F[_]] ()
		(
			implicit
			monadThrow : MonadThrow[F],
			pointcut : Pointcut[F],
			shim : F[Assertion] => Future[Assertion]
		)
		: Unit =
	{
		after[F] (pointcut)
		afterF[F] (pointcut)
		always[F] (pointcut)
		alwaysF[F] (pointcut)
		around[F] (pointcut)
		aroundF[F] (pointcut)
		before[F] (pointcut)
		beforeF[F] (pointcut)
		bracket[F] (pointcut)
		finalizeWith[F] (pointcut)
	}


	private def after[F[_]] (pointcut : Pointcut[F])
		(
			implicit
			monadThrow : MonadThrow[F],
			shim : F[Assertion] => Future[Assertion]
		)
		: Unit =
	{
		"be able to invoke 'after' logic" in {
			val result = pointcut.after (Now (2.pure[F])) (_.squared)

			result.value map {
				after =>
					assert (after === 4)
				}
			}

		"only evaluate F[ResultT] once in 'after' logic" in {
			val result = pointcut.after (Later (DetectUsage ().pure[F])) (
				_.use ()
				)

			result.value map {
				usage =>
					assert (usage.count === 1)
				}
			}

		"not evaluate 'after' logic in the presence of errors" in {
			val error = SimulatedError ("simulated")
			val result = pointcut.after (Later (error.raiseError[F, Int])) {
				_ => fail ("after logic was evaluated")
				}

			verifyErrorSkipsLeaving (result)
			}
	}


	private def afterF[F[_]] (pointcut : Pointcut[F])
		(
			implicit
			monadThrow : MonadThrow[F],
			shim : F[Assertion] => Future[Assertion]
		)
		: Unit =
	{
		"be able to invoke 'afterF' logic" in {
			val result = pointcut.afterF (Now (2.pure[F])) {
				_.squared.pure[F]
				}

			result.value map {
				after =>
					assert (after === 4)
				}
			}

		"only evaluate F[ResultT] once in 'afterF' logic" in {
			val result = pointcut.afterF (Later (DetectUsage ().pure[F])) {
				_.use ().pure[F]
				}

			result.value map {
				usage =>
					assert (usage.count === 1)
				}
			}

		"not evaluate 'afterF' logic in the presence of errors" in {
			val error = SimulatedError ("simulated")
			val result = pointcut.afterF (Later (error.raiseError[F, Int])) {
				_ => fail ("after logic was evaluated")
				}

			verifyErrorSkipsLeaving (result)
			}

		"fail the evaluation if 'afterF' logic fails" in {
			val result = pointcut.afterF (Now (1.pure[F])) {
				_ => SimulatedError ("afterF failed").raiseError
				}

			verifyWhenError (result) {
				case Left (SimulatedError (_)) =>
					succeed

				case Left (unexpected) =>
					fail ("unexpected pointcut error", unexpected)

				case _ =>
					fail ("a result should not have been produced")
				}
			}
	}


	private def always[F[_]] (pointcut : Pointcut[F])
		(
			implicit
			monadThrow : MonadThrow[F],
			shim : F[Assertion] => Future[Assertion]
		)
		: Unit =
	{
		"be able to invoke leaving 'always' logic" in {
			val result = pointcut.always (Later (2.pure[F])) (
				leaving = _.squared,
				onError = _.raiseError
				)

			result.value map {
				value =>
					assert (value === 4)
				}
			}

		"be able to invoke onError 'always' logic" in {
			val handled = DetectUsage ()
			val error = SimulatedError ("simulated")
			val result = pointcut.always (Now (error.raiseError[F, Int])) (
				leaving = _ => fail ("called leaving"),
				onError = _ =>
					handled.use ()
						.pure[F]
						.void
					)

			verifyWhenError (result) {
				case Left (SimulatedError (_)) =>
					assert (handled.count === 1)

				case Left (unexpected) =>
					fail ("unexpected pointcut error", unexpected)

				case _ =>
					fail ("a result should not have been produced")
					}
				}

		"not invoke leaving logic in 'always' when error is present" in {
			val error = SimulatedError ("should stop happy path")
			val result = pointcut.always (Now (error.raiseError[F, Int])) (
				leaving = _ => fail ("called leaving"),
				onError = _.raiseError
				)

			verifyWhenError (result) {
				case Left (SimulatedError (_)) =>
					succeed

				case other =>
					fail ("should have had simulated error: " + other)
				}
			}
	}


	private def alwaysF[F[_]] (pointcut : Pointcut[F])
		(
			implicit
			monadThrow : MonadThrow[F],
			shim : F[Assertion] => Future[Assertion]
		)
		: Unit =
	{
		"be able to invoke leaving 'alwaysF' logic" in {
			val result = pointcut.alwaysF (Later (2.pure[F])) (
				leaving = _.squared.pure[F],
				onError = _.raiseError
				)

			result.value map {
				value =>
					assert (value === 4)
				}
			}

		"be able to invoke onError 'alwaysF' logic" in {
			val handled = DetectUsage ()
			val error = SimulatedError ("simulated")
			val result = pointcut.alwaysF (Now (error.raiseError[F, Int])) (
				leaving = _ => fail ("called leaving"),
				onError = _ =>
					handled.use ()
						.pure[F]
						.void
				)

			verifyWhenError (result) {
				case Left (SimulatedError (_)) =>
					assert (handled.count === 1)

				case Left (unexpected) =>
					fail ("unexpected pointcut error", unexpected)

				case _ =>
					fail ("a result should not have been produced")
				}
			}

		"not invoke leaving logic in 'alwaysF' when error is present" in {
			val error = SimulatedError ("should stop happy path")
			val result = pointcut.alwaysF (Now (error.raiseError[F, Int])) (
				leaving = _ => fail ("called leaving"),
				onError = _.raiseError
				)

			verifyWhenError (result) {
				case Left (SimulatedError (_)) =>
					succeed

				case other =>
					fail ("should have had simulated error: " + other)
				}
			}
	}


	private def around[F[_]] (pointcut : Pointcut[F])
		(
			implicit
			monadThrow : MonadThrow[F],
			shim : F[Assertion] => Future[Assertion]
		)
		: Unit =
	{
		"be able to invoke before and after logic in 'around'" in {
			val before = DetectUsage ()
			val result = pointcut.around (Now (2.pure[F])) (
				entering = () => before.use (),
				leaving = _.squared,
				onError = _.raiseError
				)

			assert (before.count === 0)

			result.value map {
				value =>
					assert (value === 4)
					assert (before.count === 1)
				}
			}

		"evaluate before logic in 'around' when F fails" in {
			val before = DetectUsage ()
			val error = SimulatedError ("simulated")
			val result = pointcut.around (Later (error.raiseError[F, Int])) (
				entering = () => before.use (),
				leaving = _.squared,
				onError = _.raiseError
				)

			assert (before.count === 0)

			verifyWhenError (result) {
				errorOrInt =>
					assert (errorOrInt.isLeft)
					assert (before.count === 1)
				}
			}

		"be able to invoke onError 'around' logic" in {
			val handled = DetectUsage ()
			val error = SimulatedError ("simulated")
			val result = pointcut.around (Now (error.raiseError[F, Int])) (
				entering = () => {},
				leaving = _ => fail ("called leaving"),
				onError = _ =>
					handled.use ()
						.pure[F]
						.void
				)

			verifyWhenError (result) {
				case Left (SimulatedError (_)) =>
					assert (handled.count === 1)

				case Left (unexpected) =>
					fail ("unexpected pointcut error", unexpected)

				case _ =>
					fail ("a result should not have been produced")
			}
		}

		"not invoke leaving logic in 'around' when error is present" in {
			val before = DetectUsage ()
			val error = SimulatedError ("should stop happy path")
			val result = pointcut.around (Now (error.raiseError[F, Int])) (
				entering = () => before.use (),
				leaving = _ => fail ("called leaving"),
				onError = _.raiseError
				)

			assert (before.count === 0)

			verifyWhenError (result) {
				case Left (SimulatedError (_)) =>
					assert (before.count === 1)

				case other =>
					fail ("should have had simulated error: " + other)
				}
			}
	}


	private def aroundF[F[_]] (pointcut : Pointcut[F])
		(
			implicit
			monadThrow : MonadThrow[F],
			shim : F[Assertion] => Future[Assertion]
		)
	: Unit =
	{
		"be able to invoke before and after logic in 'aroundF'" in {
			val before = DetectUsage ()
			val result = pointcut.aroundF (Now (2.pure[F])) (
				entering = () =>
					before.use ()
						.pure[F]
						.void,

				leaving = _.squared.pure[F],
				onError = _.raiseError
				)

			assert (before.count === 0)

			result.value map {
				value =>
					assert (value === 4)
					assert (before.count === 1)
				}
			}

		"evaluate before logic in 'aroundF' when F fails" in {
			val before = DetectUsage ()
			val error = SimulatedError ("simulated")
			val result = pointcut.aroundF (Later (error.raiseError[F, Int])) (
				entering = () =>
					before.use ()
						.pure[F]
						.void,

				leaving = _.squared.pure[F],
				onError = _.raiseError
				)

			assert (before.count === 0)

			verifyWhenError (result) {
				errorOrInt =>
					assert (errorOrInt.isLeft)
					assert (before.count === 1)
				}
			}

		"be able to invoke before and onError 'aroundF' logic" in {
			val before = DetectUsage ()
			val handled = DetectUsage ()
			val error = SimulatedError ("simulated")
			val result = pointcut.aroundF (Now (error.raiseError[F, Int])) (
				entering = () =>
					before.use ()
						.pure[F]
						.void,

				leaving = _ => fail ("called leaving"),
				onError = _ =>
					handled.use ()
						.pure[F]
						.void
				)

			assert (before.count === 0)
			assert (handled.count === 0)

			verifyWhenError (result) {
				case Left (SimulatedError (_)) =>
					assert (before.count === 1)
					assert (handled.count === 1)

				case Left (unexpected) =>
					fail ("unexpected pointcut error", unexpected)

				case _ =>
					fail ("a result should not have been produced")
				}
			}

		"not invoke leaving logic in 'aroundF' when error is present" in {
			val before = DetectUsage ()
			val error = SimulatedError ("should stop happy path")
			val result = pointcut.aroundF (Now (error.raiseError[F, Int])) (
				entering = () =>
					before.use ()
						.pure[F]
						.void,

				leaving = _ => fail ("called leaving"),
				onError = _.raiseError
				)

			assert (before.count === 0)

			verifyWhenError (result) {
				case Left (SimulatedError (_)) =>
					assert (before.count === 1)

				case other =>
					fail ("should have had simulated error: " + other)
				}
			}
	}


	private def before[F[_]] (pointcut : Pointcut[F])
		(
			implicit
			monadThrow : MonadThrow[F],
			shim : F[Assertion] => Future[Assertion]
		)
		: Unit =
	{
		"be able to invoke 'before' logic" in {
			val before = DetectUsage ()
			val result = pointcut.before (Now (2.pure[F])) (() => before.use ())

			assert (before.count === 0)

			result.value map {
				value =>
					assert (value === 2)
					assert (before.count === 1)
				}
			}

		"evaluate 'before' logic when F fails" in {
			val before = DetectUsage ()
			val error = SimulatedError ("simulated")
			val result = pointcut.before (Later (error.raiseError[F, Int])) (
				() => before.use ()
				)

			assert (before.count === 0)

			verifyWhenError (result) {
				errorOrInt =>
					assert (errorOrInt.isLeft)
					assert (before.count === 1)
				}
			}
	}


	private def beforeF[F[_]] (pointcut : Pointcut[F])
		(
			implicit
			monadThrow : MonadThrow[F],
			shim : F[Assertion] => Future[Assertion]
		)
		: Unit =
	{
		"be able to invoke 'beforeF' logic" in {
			val before = DetectUsage ()
			val result = pointcut.beforeF (Now (2.pure[F])) {
				() =>
					before.use ()
						.pure[F]
						.void
				}

			assert (before.count === 0)

			result.value map {
				value =>
					assert (value === 2)
					assert (before.count === 1)
				}
			}

		"evaluate 'beforeF' logic when F fails" in {
			val before = DetectUsage ()
			val error = SimulatedError ("simulated")
			val result = pointcut.beforeF (Later (error.raiseError[F, Int])) {
				() =>
					before.use ()
						.pure[F]
						.void
				}

			assert (before.count === 0)

			verifyWhenError (result) {
				errorOrInt =>
					assert (errorOrInt.isLeft)
					assert (before.count === 1)
				}
			}

		"fail the evaluation if 'beforeF' logic fails" in {
			val result = pointcut.beforeF (Now (1.pure[F])) {
				() => SimulatedError ("afterF failed").raiseError
				}

			verifyWhenError (result) {
				case Left (SimulatedError (_)) =>
					succeed

				case Left (unexpected) =>
					fail ("unexpected pointcut error", unexpected)

				case _ =>
					fail ("a result should not have been produced")
				}
			}
	}


	private def bracket[F[_]] (pointcut : Pointcut[F])
		(
			implicit
			monadThrow : MonadThrow[F],
			shim : F[Assertion] => Future[Assertion]
		)
		: Unit =
	{
		"call acquire and release with 'bracket' without errors" in {
			val acquire = DetectUsage ()
			val release = DetectUsage ()
			val result = pointcut.bracket (Now (1.pure[F])) (
				() =>
					acquire.use ()
						.asRight
				) {
				_ => errorOrInt => {
					assert (errorOrInt.isRight)
					release.use ()
					}
				}

			assert (acquire.count === 0)
			assert (release.count === 0)

			result.value map {
				value =>
					assert (value === 1)
					assert (acquire.count === 1)
					assert (release.count === 1)
				}
			}

		"call acquire and release with 'bracket' with errors" in {
			val acquire = DetectUsage ()
			val release = DetectUsage ()
			val error = SimulatedError ("simulated")
			val result = pointcut.bracket (Now (error.raiseError[F, Int])) (
				() =>
					acquire.use ()
						.asRight
				) {
				_ => errorOrInt => {
					assert (errorOrInt.isLeft)
					release.use ()
					}
				}

			assert (acquire.count === 0)
			assert (release.count === 0)

			verifyWhenError (result) {
				case Left (SimulatedError (_)) =>
					assert (acquire.count === 1)
					assert (release.count === 1)

				case other =>
					fail ("simulated error was not propagated: " + other)
				}
			}
	}


	private def finalizeWith[F[_]] (pointcut : Pointcut[F])
		(
			implicit
			monadThrow : MonadThrow[F],
			shim : F[Assertion] => Future[Assertion]
		)
		: Unit =
	{
		"invoke the functor given to 'finalizeWith' without errors" in {
			val finalizer = DetectUsage ()
			val result = pointcut.finalizeWith (Later (2.pure[F])) {
				() =>
					finalizer.use ()
						.pure[F]
						.void
				}

			result.value map {
				value =>
					assert (value === 2)
					assert (finalizer.count === 1)
				}
			}

		"invoke the functor given to 'finalizeWith' with errors" in {
			val finalizer = DetectUsage ()
			val error = SimulatedError ("simulated")
			val result = pointcut.finalizeWith (Later (error.raiseError[F, Int])) {
				() =>
					finalizer.use ()
						.pure[F]
						.void
				}

			verifyWhenError (result) {
				case Left (SimulatedError (_)) =>
					assert (finalizer.count === 1)

				case other =>
					fail ("simulated error was not propagated: " + other)
				}
			}
	}


	private def verifyErrorSkipsLeaving[F[_], A] (efa : Eval[F[A]])
		(
			implicit
			monadThrow : MonadThrow[F],
			shim : F[Assertion] => Future[Assertion]
		)
		: Future[Assertion] =
		verifyWhenError (efa) {
			case Left (SimulatedError (_)) =>
				succeed

			case Left (unexpected) =>
				fail ("unexpected pointcut error", unexpected)

			case _ =>
				fail ("evaluation should not have happened")
			}


	private def verifyWhenError[F[_], A] (efa : Eval[F[A]])
		(check : Either[Throwable, A] => Assertion)
		(
			implicit
			monadThrow : MonadThrow[F],
			shim : F[Assertion] => Future[Assertion]
		)
		: Future[Assertion] =
		efa.value
			.attempt
			.map (check)
}


object PointcutBehaviours
{
	/// Class Types
	final case class DetectUsage ()
	{
		/// Instance Properties
		def count = counter

		@volatile
		private var counter : Int = 0


		def use () : DetectUsage =
		{
			counter = counter + 1
			this
		}
	}


	final case class SimulatedError (private val message : String)
		extends RuntimeException (message)
}

