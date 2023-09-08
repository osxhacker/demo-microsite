package com.github.osxhacker.demo.chassis.monitoring.logging

import scala.concurrent.duration._
import scala.language.postfixOps

import cats.{
	ApplicativeThrow,
	FlatMap,
	Later
	}

import cats.effect.IO

import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AsyncWordSpec
import org.slf4j.event.Level
import org.typelevel.log4cats.LoggerFactory

import com.github.osxhacker.demo.chassis.effect.DefaultAdvice
import com.github.osxhacker.demo.chassis.monitoring.metrics.MetricsAdvice


/**
 * The '''LogSlowInvocationSpec ''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.monitoring.logging.LogSlowInvocation]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class LogSlowInvocationSpec ()
	extends AsyncWordSpec
		with AsyncIOSpec
		with Diagrams
{
	/// Class Imports
	import cats.syntax.applicativeError._


	/// Class Types
	final case class LoggingAdvice[A] (
		private val underlying : MockLoggerFactory[IO]
		)
		(
			implicit
			override protected val applicativeThrow : ApplicativeThrow[IO],
			override implicit protected val flatMap : FlatMap[IO]
		)
		extends DefaultAdvice[IO, A]
			with MetricsAdvice[IO, A]
			with LogInvocation[IO, A]
			with LogSlowInvocation[IO, A]
	{
		/// Instance Properties
		override protected val loggerFactory : LoggerFactory[IO] = underlying
		override val operation : String = "test-logging"
	}


	"The LogSlowInvocation advice" must {
		"emit an when execution fails" in {
			val mockLogger = MockLoggerFactory[IO] ()
			val advice = LoggingAdvice[Int] (mockLogger)
			val result = advice (
				Later (new RuntimeException ().raiseError[IO, Int])
				)

			result.value
				.attempt
				.map {
					case Left (_) =>
						assert (mockLogger.size === 1)
						assert (mockLogger.head.getLevel == Level.ERROR)

					case Right (_) =>
						fail ("error was not propagated")
				}
		}

		"not emit a warning or error when executing much less than 400ms" in {
			val mockLogger = MockLoggerFactory[IO] ()
			val advice = LoggingAdvice[Int] (mockLogger)
			val result = advice (Later (IO (42)))

			result.value map {
				i =>
					assert (i === 42)
					assert (mockLogger.isEmpty)
				}
			}

		"not emit a warning or error when executing near 400ms" in {
			val mockLogger = MockLoggerFactory[IO] ()
			val advice = LoggingAdvice[Int] (mockLogger)
			val result = advice (
				Later (IO.sleep (100 milliseconds) >> IO (42))
				)

			result.value map {
				i =>
					assert (i === 42)
					assert (mockLogger.isEmpty)
				}
			}

		"emit a warning when execution is between 400ms and 1s" in {
			val mockLogger = MockLoggerFactory[IO] ()
			val advice = LoggingAdvice[Int] (mockLogger)
			val result = advice (
				Later (IO.sleep (600 milliseconds) >> IO (42))
				)

			result.value map {
				i =>
					assert (i === 42)
					assert (mockLogger.size === 1)
					assert (mockLogger.head.getLevel == Level.WARN)
				}
			}

		"emit an error when execution is more than 1s" in {
			val mockLogger = MockLoggerFactory[IO] ()
			val advice = LoggingAdvice[Int] (mockLogger)
			val result = advice (
				Later (IO.sleep (1_100 milliseconds) >> IO (42))
				)

			result
				.value map {
				i =>
					assert (i === 42)
					assert (mockLogger.size === 1)
					assert (mockLogger.head.getLevel == Level.ERROR)
				}
			}
	}
}

