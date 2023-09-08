package com.github.osxhacker.demo.chassis.monitoring.logging

import java.time.{
	Duration => JDuration,
	Instant
	}

import java.util.concurrent.atomic.AtomicReference

import cats.Eval
import org.typelevel.log4cats
import org.typelevel.log4cats.StructuredLogger

import com.github.osxhacker.demo.chassis.effect.{
	Advice,
	Pointcut
	}

import com.github.osxhacker.demo.chassis.monitoring.metrics.MetricsAdvice


/**
 * The '''LogSlowInvocation''' type defines an
 * [[com.github.osxhacker.demo.chassis.effect.Advice]] which measures __each__
 * invocation to determine whether or not its execution time (measured in
 * "wall clock" time) exceeds one of two thresholds:
 *
 *   - `ErrorThreshold` : emit an '''ERROR''' if equal to or over ''1000ms''.
 *   - `WarningThreshold` : emit a '''WARN''' if over ''400ms'' but less than
 *     ''1000ms''.
 *
 * Invocations executing faster than the `WarningThreshold` emit nothing.
 */
trait LogSlowInvocation[F[_], ResultT]
	extends Advice[F, ResultT]
{
	/// Self Type Constraints
	this : MetricsAdvice[F, ResultT]
		with LogInvocation[F, ResultT]
		=>


	/// Class Imports
	import cats.syntax.flatMap._
	import cats.syntax.functor._
	import log4cats.syntax._
	import mouse.any._


	/// Class Types
	private object ErrorThreshold
	{
		def unapply (millis : Long) : Option[Int] =
			Option.when (millis >= 1_000) (1_000)
	}


	private object WarningThreshold
	{
		def unapply (millis : Long) : Option[Int] =
			Option.when (millis >= 400 && millis < 1_000) (400)
	}


	/// Instance Properties
	def operation : String


	abstract override def apply (fa : Eval[F[ResultT]])
		(implicit pointcut : Pointcut[F])
		: Eval[F[ResultT]] =
	{
		val when = new AtomicReference[Instant] ()

		started (super.apply (fa), when) |> (finished (_, when))
	}


	private def finished (fa : Eval[F[ResultT]], when : AtomicReference[Instant])
		(implicit pointcut : Pointcut[F])
		: Eval[F[ResultT]] =
		pointcut.afterF (fa) {
			result =>
				val duration = JDuration.between (
					when.get (),
					Instant.now ()
					)
					.toMillis
					.max (0L)

				loggerFactory.create
					.flatMap (report (duration) (_))
					.as (result)
			}


	private def report (howLong : Long)
		(implicit logger : StructuredLogger[F])
		: F[Unit] =
		howLong match {
			case ErrorThreshold (threshold) =>
				error"$operation : took ${howLong}ms (exceeded ${threshold}ms)"

			case WarningThreshold (threshold) =>
				warn"$operation : took ${howLong}ms (exceeded ${threshold}ms)"

			case _ =>
				applicativeThrow.unit
			}


	private def started (fa : Eval[F[ResultT]], when : AtomicReference[Instant])
		(implicit pointcut : Pointcut[F])
		: Eval[F[ResultT]] =
		pointcut.before (fa) (() => when.set (Instant.now ()))
}

