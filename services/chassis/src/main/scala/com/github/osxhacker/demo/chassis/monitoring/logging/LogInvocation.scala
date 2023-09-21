package com.github.osxhacker.demo.chassis.monitoring.logging

import cats.{
	Eval,
	FlatMap
	}

import org.typelevel.log4cats
import org.typelevel.log4cats.{
	Logger,
	LoggerFactory
	}

import com.github.osxhacker.demo.chassis.domain.error._
import com.github.osxhacker.demo.chassis.effect.{
	Advice,
	Pointcut
	}


/**
 * The '''LogInvocation''' type defines an
 * [[com.github.osxhacker.demo.chassis.effect.Advice]] which uses a
 * [[org.typelevel.log4cats.LoggerFactory]] to emit either `error` or `warn`
 * log entries when ''F[ResultT]'' contains an error and `debug` when
 * ''F[ResultT]'' contains success.
 *
 * It is expected that the `loggerFactory` contains any and all applicable
 * context needed for operational concerns.  Also note that the `success`
 * method does _not_ include the ''ResultT'' in its log event.  This is due to
 * the fact that some ''ResultT'' instances could have very large content, such
 * as the contents of a file.
 */
trait LogInvocation[F[_], ResultT]
	extends Advice[F, ResultT]
{
	/// Class Imports
	import cats.syntax.applicative._
	import cats.syntax.flatMap._
	import log4cats.syntax._


	/// Instance Properties
	implicit protected def flatMap : FlatMap[F]
	protected def loggerFactory : LoggerFactory[F]

	private lazy val aspectName = getClass.getSimpleName


	abstract override def apply (fa : Eval[F[ResultT]])
		(implicit pointcut : Pointcut[F])
		: Eval[F[ResultT]] =
		pointcut.alwaysF (super.apply (fa)) (_.pure[F], failed)


	private def emitLogEntry (implicit log : Logger[F]) : Throwable => F[Unit] =
	{
		case recoverable : ConflictingObjectsError[_] =>
			warn"$aspectName: ${recoverable.getMessage}"

		case recoverable : ObjectNotFoundError[_] =>
			warn"$aspectName: ${recoverable.getMessage}"

		case unrecoverable =>
			error"$aspectName: ${unrecoverable.getMessage}"
	}


	private def failed (problem : Throwable) : F[Unit] =
		loggerFactory.create >>= (emitLogEntry (_) (problem))
}

