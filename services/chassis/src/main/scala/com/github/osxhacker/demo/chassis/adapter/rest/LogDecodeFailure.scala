package com.github.osxhacker.demo.chassis.adapter.rest

import cats.Monad
import org.typelevel.log4cats.{
	StructuredLogger,
	LoggerFactory
	}


/**
 * The '''LogDecodeFailure''' type is a [[scala.Function2]] which is responsible
 * for emitting [[org.typelevel.log4cats]] log entries whenever a request
 * cannot be decoded due to payload-related errors.
 *
 * [[https://tapir.softwaremill.com/en/latest/server/debugging.html Tapir logging]]
 * has two candidates for the type of logging desired here;
 * `logAllDecodeFailures` and `logWhenHandled`.  At first glance,
 * `logAllDecodeFailures` would appear to be the ideal candidate.  However, a
 * "decode failure" in the
 * [[https://tapir.softwaremill.com/en/latest/server/http4s.html Tapir http4s]]
 * server is __actually__ called whenever candidate endpoints do not match.
 * It is the `logWhenHandled` handler, when a [[java.lang.Throwable]] is
 * present, that has been shown empirically to be the handler desired for
 * `error/warn` logging.  An example server options configuration is:
 *
 * {{{
 *     private val options = Http4sServerOptions.customiseInterceptors
 *         .serverLog (
 *             Http4sDefaultServerLog[F].doLogWhenHandled (
 *                 LogDecodeFailure[F] ()
 *                 )
 *             .logWhenHandled (true)
 *             )
 *         .options
 * }}}
 */
final case class LogDecodeFailure[F[_]] ()
	(
		implicit

		private val loggerFactory : LoggerFactory[F],

		/// Needed for `>>=`.
		private val monad : Monad[F]
	)
	extends ((String, Option[Throwable]) => F[Unit])
{
	/// Class Imports
	import cats.syntax.flatMap._


	/// Instance Properties
	private val additionalProperties = Map ("subsystem" -> "rest")


	override def apply (message : String, problem : Option[Throwable])
		: F[Unit] =
		loggerFactory.create >>= emit (message, problem)


	private def emit (message : String, problem : Option[Throwable])
		(log : StructuredLogger[F])
		: F[Unit] =
		problem.fold (monad.unit) {
			ex =>
				log.error (additionalProperties, ex) (message)
			}
}

