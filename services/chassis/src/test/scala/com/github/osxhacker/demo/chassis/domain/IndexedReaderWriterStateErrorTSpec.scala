package com.github.osxhacker.demo.chassis.domain

import java.lang.{
	Double => JDouble
	}

import scala.collection.mutable
import scala.util.Try

import cats._
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec
import com.github.osxhacker.demo.chassis.ProjectSpec


/**
 * The '''IndexedReaderWriterStateErrorTSpec''' type defines the unit-tests
 * which certify
 * [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class IndexedReaderWriterStateErrorTSpec ()
	extends AnyWordSpec
		with Diagrams
		with ProjectSpec
{
	/// Class Imports
	import cats.syntax.applicative._
	import cats.syntax.either._
	import mouse.boolean._


	/// Class Types
	type SimpleLog = List[String]


	final case class SampleEnv (val coefficient : Int)


	"The IndexedReaderWriterStateErrorT type" when {
		"being defined" must {
			"support for comprehensions via the script method" in {
				import IndexedReaderWriterStateErrorT.script


				val result = script[ErrorOr, SampleEnv, SimpleLog, Throwable] {
					combinators =>
						import combinators._

						for {
							initial <- get[Int]
							squared <- inspect[Int, Int] (n => n * n)
							_ <- inspectF[Int, Int] (_.asRight)
							_ <- inspectAskF[Int, Int] ((_, _) => 0.asRight)
							_ <- modify[Int, String] (_.toString)
							_ <- modifyF[String, JDouble] {
								sb =>
									Try (JDouble.valueOf (sb)).toEither
								}

							ending <- get
							_ <- setF (ending.asRight)
							} yield {
								assert (initial >= 0)
								assert (squared >= initial)
								assert (ending.doubleValue () === 1.0)

								ending
								}
					}

				assert (result ne null)
				assert (result.runA (SampleEnv (2), 1).isRight)
				}

			"support construction by using companion methods" in {
				import IndexedReaderWriterStateErrorT.{
					apply => _,
					applyF => _,
					_
					}


				val steps =
					inspectAsk[ErrorOr, SampleEnv, SimpleLog, Throwable, Int, Int] (
						(env, n) => env.coefficient * n
						)
						.flatMap (
							set[ErrorOr, SampleEnv, SimpleLog, Throwable, Int]
							)

				assert (steps ne null)

				val result = steps.runS (SampleEnv (3), 3)

				assert (result exists (_._1.isEmpty))
				assert (result exists (_._2.isRight))
				assert (result exists (_._2 exists (_ === 9)))
				}

			"detect errors in the underlying context and recover" in {
				import IndexedReaderWriterStateErrorT.script


				val steps = script[ErrorOr, SampleEnv, SimpleLog, Throwable] {
					combinators =>
						import combinators._

						for {
							_ <- tell[Int] ("before error".pure[List])
							_ <- liftF[Int, String] (
								ApplicativeThrow[ErrorOr].raiseError (
									new RuntimeException ("simulated")
									)
								)

							_ <- tell[Int] ("after error".pure[List])
							skipped <- pure (99)
							} yield skipped
					}

				assert (steps ne null)

				val result = steps.run (SampleEnv (1), 1)

				assert (result.isRight)
				result foreach {
					case (log, Left (error)) =>
						assert (log.size === 1)
						assert (error.getMessage === "simulated")

					case other =>
						fail (s"unexpected result shape: $other")
					}
				}
			}

		"being evaluated" must {
			import IndexedReaderWriterStateErrorT.script


			"retain log entries when there are no errors" in {
				val logEntries = Seq (
					"first log message",
					"second log message",
					"third log message"
					) map (_.pure[List])

				val steps = script[ErrorOr, SampleEnv, SimpleLog, Throwable] {
					combinators =>
						import combinators._

						for {
							_ <- tell[Int] (logEntries (0))
							initial <- get[Int]
							_ <- tell[Int] (logEntries (1))
							env <- ask
							_ <- tell[Int] (logEntries (2))
							} yield env.coefficient * initial
					}

				assert (steps ne null)

				val env = SampleEnv (10)
				val result = steps.runA (env, 2)

				assert (result.isRight)
				result foreach {
					case (log, Right (answer)) =>
						assert (log.size === logEntries.size)
						assert (log forall logEntries.flatten.contains)
						assert (answer === env.coefficient * 2)

					case other =>
						fail (s"unexpected result shape: $other")
					}
				}

			"retain log entries up to the first error and after recovery" in {
				val expectedLogEntries = Seq (
					"before error",
					"first log message",
					"second log message",
					"third log message"
					) map (_.pure[List])

				val steps = script[ErrorOr, SampleEnv, SimpleLog, Throwable] {
					combinators =>
						import combinators._

						val initial = for {
							_ <- tellF[Int] (expectedLogEntries.head.asRight)
							_ <- liftF (
								ApplicativeThrow[ErrorOr].raiseError[Int] (
									new RuntimeException ("simulated")
									)
								)

							_ <- tell ("after error".pure[List])
							} yield 0

						val alternate = for {
							_ <- tell[Int] (expectedLogEntries (1))
							initial <- get
							_ <- tell (expectedLogEntries (2))
							env <- ask
							_ <- tell (expectedLogEntries (3))
						} yield env.coefficient * initial

						initial handleErrorWith (_ => alternate)
				}

				assert (steps ne null)

				val env = SampleEnv (2)
				val result = steps.runA (env, 4)

				assert (result.isRight)
				result foreach {
					case (log, Right (answer)) =>
						assert (log.size === expectedLogEntries.size)
						assert (expectedLogEntries.flatten forall log.contains)
						assert (log.contains ("after error") === false)
						assert (answer === env.coefficient * 4)

					case other =>
						fail (s"unexpected result shape: $other")
					}
				}

			"support emitting the log when ran (success)" in {
				val emitted = mutable.Buffer.empty[String]
				val logEntries = Seq (
					"first log message",
					"second log message",
					"third log message"
					) map (_.pure[List])

				val env = SampleEnv (5)
				val steps = script[ErrorOr, SampleEnv, SimpleLog, Throwable] {
					combinators =>
						import combinators._

						for {
							_ <- tell[Int] (logEntries (0))
							initial <- get[Int]
							_ <- tell[Int] (logEntries (1))
							env <- ask
							_ <- tell[Int] (logEntries (2))
							} yield env.coefficient * initial
						}

				assert (steps ne null)

				val result = steps.runAndReportA (env, 4) {
					case (_, Left (error)) =>
						fail (s"unexpected error: $error")

					case (log, Right ((state, computation))) =>
						assert (log === logEntries.flatten)
						assert (state === 4)
						assert (computation === 20)

						emitted.appendAll (log)
						Applicative[ErrorOr].unit
					}

				assert (emitted === logEntries.flatten)
				assert (result.exists (_ === 20))
				}

			"support emitting the log when ran (error)" in {
				val emitted = mutable.Buffer.empty[String]
				val expectedLogEntries = Seq ("before error") map (_.pure[List])

				val steps = script[ErrorOr, SampleEnv, SimpleLog, Throwable] {
					combinators =>
						import combinators._

						for {
							_ <- tellF[Int] (expectedLogEntries.head.asRight)
							_ <- liftF (
								ApplicativeThrow[ErrorOr].raiseError[Int] (
									new RuntimeException ("simulated")
									)
								)

							_ <- tell ("after error".pure[List])
							env <- ask
							} yield env.coefficient
					}

				assert (steps ne null)

				val env = SampleEnv (99)
				val result = steps.runAndReportA (env, 4) {
					case (log, Left (error)) =>
						assert (log === expectedLogEntries.flatten)
						assert (error ne null)
						emitted.appendAll (log)
						Applicative[ErrorOr].unit

					case (_, answer) =>
						fail (s"unexpected success: $answer")
					}

				assert (emitted === expectedLogEntries.flatten)
				assert (result.isLeft)
				assert (result.swap.exists (_.getMessage ne null))
				}

			"support tailRecM (success)" in {
				import IndexedReaderWriterStateErrorT.{
					apply => _,
					applyF => _,
					_
					}

				val countdown =
					get[ErrorOr, SampleEnv, SimpleLog, Throwable, Int].flatMap {
						count =>
							tell (s"countdown... $count".pure[List])
						}
					.modify (_ - 1)
					.inspect (_ > 0)
					.map (_.asLeft[Int])

				val finished = pure[
					ErrorOr,
					SampleEnv,
					SimpleLog,
					Throwable,
					Int,
					Either[Boolean, Int]
					] (0.asRight[Boolean])

				val steps = Monad[
					ReaderWriterStateErrorT[
						ErrorOr,
						SampleEnv,
						SimpleLog,
						Throwable,
						Int,
						*
						]
					].tailRecM (true) (_.fold (countdown, finished))

				steps.runA (SampleEnv (1), 10) foreach {
					case (_, Left (error)) =>
						fail ("unexpected error detected", error)

					case (log, Right (result)) =>
						assert (log.size === 10)
						assert (result === 0)
					}
				}

			"support tailRecM (error)" in {
				import IndexedReaderWriterStateErrorT.{
					apply => _,
					applyF => _,
					_
					}

				val countdown =
					get[ErrorOr, SampleEnv, SimpleLog, Throwable, Int].flatMap {
						count =>
							tell (s"countdown... $count".pure[List])
						}
						.modify (_ - 1)
						.inspect (_ > 0)
						.map (_.asLeft[Int])

				val produceError = liftF[
					ErrorOr,
					SampleEnv,
					SimpleLog,
					Throwable,
					Int,
					Either[Boolean, Int]
					] (
					ApplicativeThrow[ErrorOr].raiseError (
						new RuntimeException ()
						)
					)

				val steps = Monad[
					ReaderWriterStateErrorT[
						ErrorOr,
						SampleEnv,
						SimpleLog,
						Throwable,
						Int,
						*
						]
					].tailRecM (true) (_.fold (countdown, produceError))

				steps.runA (SampleEnv (1), 10) foreach {
					case (log, Left (_)) =>
						assert (log.size === 10)

					case (_, Right (result)) =>
						fail (s"expected an error not success: $result")
					}
				}
			}

		"participating in Cats type classes" must {
			type SampleIndexedType[SA, SB, A] = IndexedReaderWriterStateErrorT[
				ErrorOr,
				SampleEnv,
				SimpleLog,
				Throwable,
				SA,
				SB,
				A
				]


			"provide a Applicative instance" in {
				val instance = implicitly[
					Applicative[SampleIndexedType[Any, Any, *]]
					]

				assert (instance ne null)
				}

			"provide a ApplicativeError instance" in {
				val instance = implicitly[
					ApplicativeError[
						SampleIndexedType[Unit, Unit, *],
						Throwable
						]
					]

				assert (instance ne null)
				}

			"provide a Bifunctor instance" in {
				val instance = implicitly[
					Bifunctor[SampleIndexedType[String, *, *]]
					]

				assert (instance ne null)
				}

			"provide a Contravariant instance" in {
				val instance = implicitly[
					Contravariant[SampleIndexedType[*, String, Boolean]]
					]

				assert (instance ne null)
				}

			"provide a Functor instance" in {
				val instance = implicitly[
					Functor[SampleIndexedType[String, Int, *]]
					]

				assert (instance ne null)
				}

			"provide a MonadError instance" in {
				val instance = implicitly[
					MonadError[SampleIndexedType[String, String, *], Throwable]
					]

				assert (instance ne null)
				}

			"provide a Monad instance" in {
				val instance = implicitly[
					Monad[SampleIndexedType[String, String, *]]
					]

				assert (instance ne null)
				}
			}
		}
}

