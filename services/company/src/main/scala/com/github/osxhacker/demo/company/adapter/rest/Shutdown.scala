package com.github.osxhacker.demo.company.adapter.rest

import scala.language.postfixOps

import cats.MonadThrow
import cats.data.StateT
import org.typelevel.log4cats
import org.typelevel.log4cats.{
	Logger,
	LoggerFactory,
	SelfAwareStructuredLogger
	}

import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.server.ServerEndpoint

import com.github.osxhacker.demo.chassis.adapter.ServiceDeactivator
import com.github.osxhacker.demo.chassis.adapter.rest.Path
import com.github.osxhacker.demo.chassis.effect.ReadersWriterResource
import com.github.osxhacker.demo.company.adapter.RuntimeSettings
import com.github.osxhacker.demo.company.domain.{
	GlobalEnvironment,
	ScopedEnvironment
	}

import com.github.osxhacker.demo.company.adapter.rest.api._


/**
 * The '''Shutdown''' type defines the [[sttp.tapir.Endpoint]]s relating to
 * providing REST
 * [[com.github.osxhacker.demo.company.adapter.rest.api.Endpoints.Internal]]
 * shutdown resources.
 */
final case class Shutdown[F[_]] (
	override protected val settings : RuntimeSettings,
	private val halt : ServiceDeactivator[F]
	)
	(
		implicit
		/// Needed for `serverLogicWithEnvironment`.
		override protected val environment : ReadersWriterResource[
			F,
			GlobalEnvironment[F]
			],

		/// Needed for '''AbstractResource'''.
		override protected val loggerFactory : LoggerFactory[F],
		override protected val monadThrow : MonadThrow[F]
	)
	extends AbstractResource[F] ()
{
	/// Class Imports
	import cats.syntax.apply._
	import cats.syntax.flatMap._
	import cats.syntax.functor._
	import log4cats.syntax._


	/// Instance Properties
	private[rest] val put : ServerEndpoint[Any, F] =
		Endpoints.Internal
			.putInternalShutdown
			.extractPath ()
			.out (statusCode)
			.errorOut (statusCode)
			.publishUnderApi ()
			.serverLogic {
				(initiate _).tupled
				}


	override def apply () : List[ServerEndpoint[Any, F]] =
		put ::
		Nil


	private def attemptToQuiesce (message : ShutdownMessage)
		(implicit logger : Logger[F])
		: StateT[F, GlobalEnvironment[F], ResultType[StatusCode]] =
		StateT.inspect[F, GlobalEnvironment[F], Boolean] (_.isOnline)
			.ifM (quiesce (message.message.value), reject ())


	private def initiate (
		params : Endpoints.Internal.PutInternalShutdownParams,
		payload : ShutdownMessage,
		path : Path
		)
		: F[ResultType[StatusCode]] =
		environment.writer {
			import StateT._


			for {
				global <- get[F, GlobalEnvironment[F]]
				scoped <- inspectF[F, GlobalEnvironment[F], ScopedEnvironment[F]] (
					implicit env =>
						createScopedEnvironment (path, params)
					)

				implicit0 (logger : SelfAwareStructuredLogger[F]) <- liftF {
					scoped.loggingFactory
						.create
						.map {
							_.addContext (
								Map (
									"forwarded" -> params.`X-Forwarded-For`
										.getOrElse ("N/A"),

									"user-agent" -> params.`User-Agent`
									)
								)
							}
					}

				_ <- infoS (s"initiating shutdown isOnline=${global.isOnline}")
				result <- attemptToQuiesce (payload)
				} yield result
			} <* halt.signal (payload.message)


	private def infoS[S] (message : String)
		(implicit logger : Logger[F])
		: StateT[F, S, Unit] =
		StateT.liftF {
			logger.info (
				message.stripMargin
					.replace ("\n", "")
				)
			}


	private def quiesce (message : String)
		(implicit logger : Logger[F])
		: StateT[F, GlobalEnvironment[F], ResultType[StatusCode]] =
		for {
			_ <- infoS (s"quiescing service message=$message")
			_ <- StateT.modify[F, GlobalEnvironment[F]] (_.quiesce (message))
			updated <- StateT.liftF (complete (StatusCode.Ok))
			} yield updated


	private def reject ()
		(implicit logger : Logger[F])
		: StateT[F, GlobalEnvironment[F], ResultType[StatusCode]] =
		StateT.liftF {
			error"attempting to shutdown while shutting down" >>
				failWith (
					InvalidModelStateDetails.from (
						title = "service already shutting down"
					)
				)
			}
}

