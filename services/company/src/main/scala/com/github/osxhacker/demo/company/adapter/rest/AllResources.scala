package com.github.osxhacker.demo.company.adapter.rest

import cats.effect.Async
import org.http4s.HttpApp
import org.http4s.server.Router
import org.typelevel.log4cats.LoggerFactory
import sttp.tapir.server.http4s.{
	Http4sDefaultServerLog,
	Http4sServerInterpreter,
	Http4sServerOptions
	}

import com.github.osxhacker.demo.chassis.adapter.ServiceDeactivator
import com.github.osxhacker.demo.chassis.adapter.rest.LogDecodeFailure
import com.github.osxhacker.demo.chassis.effect.{
	Pointcut,
	ReadersWriterResource
	}

import com.github.osxhacker.demo.company.adapter.RuntimeSettings
import com.github.osxhacker.demo.company.domain.GlobalEnvironment


/**
 * The '''AllResources''' type provides all available
 * [[com.github.osxhacker.demo.company.adapter.rest]] resources within
 * the container [[org.http4s.HttpApp]] parameterized by ''F''.
 */
final case class AllResources[F[_]] (
	private val settings : RuntimeSettings,
	private val halt : ServiceDeactivator[F]
	)
	(
		implicit
		private val async : Async[F],
		private val loggerFactory : LoggerFactory[F],
		private val pointcut : Pointcut[F],
		private val environment : ReadersWriterResource[F, GlobalEnvironment[F]]
	)
	extends (() => HttpApp[F])
{
	/// Instance Properties
	private val reporter = SystemErrorReporter[F] ()
	private val options = Http4sServerOptions.customiseInterceptors
		.defaultHandlers (reporter.defaultFailureResponse)
		.exceptionHandler (reporter)
		.serverLog (
			Http4sDefaultServerLog[F].doLogWhenHandled (
				LogDecodeFailure[F] ()
				)
				.logWhenHandled (true)
			)
		.options

	private val companyCollection = CompanyCollection[F] (settings)
	private val companyResource = CompanyResource[F] (settings)
	private val heartbeat = Heartbeat[F] (settings)
	private val shutdown = Shutdown[F] (settings, halt)
	private val routes = Http4sServerInterpreter[F] (options).toRoutes (
		companyResource () :::
		companyCollection () :::
		heartbeat () :::
		shutdown ()
		)


	override def apply () : HttpApp[F] = Router[F] ("/" -> routes).orNotFound
}

