package com.github.osxhacker.demo.storageFacility.adapter.rest

import cats.MonadThrow
import org.typelevel.log4cats.LoggerFactory
import sttp.tapir._
import sttp.tapir.server.ServerEndpoint

import com.github.osxhacker.demo.chassis.effect.{
	Pointcut,
	ReadersWriterResource
	}

import com.github.osxhacker.demo.storageFacility.adapter.rest.api.Endpoints
import com.github.osxhacker.demo.storageFacility.adapter.RuntimeSettings
import com.github.osxhacker.demo.storageFacility.domain.GlobalEnvironment


/**
 * The '''Heartbeat''' type defines the [[sttp.tapir.Endpoint]]s relating to
 * providing REST
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.api.Endpoints.Internal]]
 * heartbeat resources.
 */
final case class Heartbeat[F[_]] (
	override protected val settings : RuntimeSettings
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
		override protected val monadThrow : MonadThrow[F],

		/// Needed for `serverLogicWithEnvironment`.
		private val pointcut : Pointcut[F]
	)
	extends AbstractResource[F] ()
{
	/// Class Imports
	import Endpoints.Internal.GetInternalHeartbeatParams
	import cats.syntax.flatMap._
	import cats.syntax.functor._


	/// Instance Properties
	private[rest] val get : ServerEndpoint[Any, F] =
		Endpoints.Internal
			.getInternalHeartbeat
			.errorOut (statusCode)
			.publishUnderApi ()
			.serverLogicWithEnvironment[GlobalEnvironment[F]] {
				implicit global =>
					evaluate
				}

	private val path = get.showPathTemplate ()


	override def apply () : List[ServerEndpoint[Any, F]] =
		get ::
		Nil


	private def evaluate (params : GetInternalHeartbeatParams)
		(implicit global : GlobalEnvironment[F])
		: F[ResultType[Unit]] =
		for {
			env <- global.scopeWith (global.operations, params.`X-Correlation-ID`)
			log <- env.loggingFactory.create
			_ <- log.debug (s"storage facility available path=$path")
			result <- complete ()
			} yield result
}

