package com.github.osxhacker.demo.chassis.domain

import java.lang.{
	Integer => JInteger
	}

import scala.util.Try

import cats._
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec
import com.github.osxhacker.demo.chassis.ProjectSpec


/**
 * The '''ReaderWriterStateErrorTSpec''' type defines the unit-tests which
 * certify [[com.github.osxhacker.demo.chassis.domain.ReaderWriterStateErrorT]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class ReaderWriterStateErrorTSpec ()
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


	"The ReaderWriterStateErrorT type" when {
		"being defined" must {
			"support for comprehensions via the script method" in {
				import ReaderWriterStateErrorT.script


				val result = script[ErrorOr, SampleEnv, SimpleLog, Throwable, Int] {
					combinators =>
						import combinators._

						for {
							initial <- get
							squared <- inspect (n => n * n)
							_ <- inspectF (_.asRight)
							_ <- inspectAskF ((_, _) => 0.asRight)
							_ <- modify (identity)
							_ <- modifyF {
								i =>
									Try (
										JInteger.valueOf (i.toString).toInt
										).toEither
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
				import ReaderWriterStateErrorT._


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
				import ReaderWriterStateErrorT.script


				val steps = script[ErrorOr, SampleEnv, SimpleLog, Throwable, Int] {
					combinators =>
						import combinators._

						for {
							_ <- tell ("before error".pure[List])
							_ <- liftF[Int] (
								ApplicativeThrow[ErrorOr].raiseError (
									new RuntimeException ("simulated")
									)
								)

							_ <- tell ("after error".pure[List])
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

		"participating in Cats type classes" must {
			type SampleStateType[S, A] = ReaderWriterStateErrorT[
				ErrorOr,
				SampleEnv,
				SimpleLog,
				Throwable,
				S,
				A
				]


			"provide a Applicative instance" in {
				val instance = implicitly[
					Applicative[SampleStateType[Any, *]]
					]

				assert (instance ne null)
				}

			"provide a ApplicativeError instance" in {
				val instance = implicitly[
					ApplicativeError[
						SampleStateType[Unit, *],
						Throwable
						]
					]

				assert (instance ne null)
				}

			"provide a Functor instance" in {
				val instance = implicitly[
					Functor[SampleStateType[String, *]]
					]

				assert (instance ne null)
				}

			"provide a MonadError instance" in {
				val instance = implicitly[
					MonadError[SampleStateType[String, *], Throwable]
					]

				assert (instance ne null)
				}

			"provide a Monad instance" in {
				val instance = implicitly[
					Monad[SampleStateType[String, *]]
					]

				assert (instance ne null)
				}
			}
		}
}

