package com.github.osxhacker.demo.storageFacility.adapter

import scala.language.postfixOps

import cats.Monad
import cats.effect._
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import eu.timepit.refined
import kamon.Kamon
import org.http4s.blaze.server.BlazeServerBuilder
import org.typelevel.log4cats
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

import com.github.osxhacker.demo.chassis.adapter.{
	ProgramArguments,
	ServiceDeactivator
	}

import com.github.osxhacker.demo.chassis.effect.ReadersWriterResource
import com.github.osxhacker.demo.storageFacility.adapter.kafka.AllConsumers
import com.github.osxhacker.demo.storageFacility.domain.GlobalEnvironment


/**
 * The '''Server''' `object` defines the entrypoint for the storage facility
 * process.
 */
object Server
	extends CommandIOApp (
		name = "storage-facility-service",
		header = "Storage facility microservice."
		)
		with ProgramArguments
{
	/// Class Imports
	import ProgramArguments.OperatingMode
	import cats.syntax.flatMap._
	import log4cats.syntax._
	import mouse.boolean._
	import refined.auto._


	/// Instance Properties
	implicit private val logFactory : LoggerFactory[IO] = Slf4jFactory[IO]


	override def main : Opts[IO[ExitCode]] =
		arguments () map {
			case mode : OperatingMode =>
				configureLogging (mode.loggingLayout, mode.verbose) >>
				IO.delay (Kamon.init ()) >>
				Monad[IO].flatMap2 (
					RuntimeSettings[IO] (mode.settings.value),
					ServiceDeactivator[IO] ()
					) (services)

			case other =>
				IO.raiseError (
					new RuntimeException (
						s"unknown program argument detected: $other"
						)
					)
					.as (ExitCode.Error)
			}


	/**
	 * The configureLogging method ensures the Logback environment properties
	 * are configured to what the '''Server''' was invoked with.
	 */
	private def configureLogging (layout : String, verbose : Boolean)
		: IO[Unit] =
		IO.delay (System.setProperty ("LOG_LAYOUT", layout.toUpperCase)) >>
			IO.delay (
				System.setProperty (
					"LOG_LEVEL",
					verbose.fold ("DEBUG", "INFO")
					)
				)


	/**
	 * The http method defines the ability to expose RESTful resources as
	 * defined by the service's RAML API.
	 */
	private def http (
		settings : RuntimeSettings,
		deactivator : ServiceDeactivator[IO]
		)
		(implicit envResource : ReadersWriterResource[IO, GlobalEnvironment[IO]])
		: IO[ExitCode] =
	{
		val allResources = rest.AllResources[IO] (settings, deactivator)

		BlazeServerBuilder[IO]
			.withIdleTimeout (settings.idleTimeout.value)
			.withHttpApp (allResources ())
			.bindHttp (
				host = settings.http.address.value,
				port = settings.http.port.value
				)
			.resource
			.use {
				server =>
					for {
						logger <- LoggerFactory.create[IO]
						_ <- logger.info (
							s"starting http service uri=${server.baseUri}"
							)

						reason <- deactivator.await ()

						(why, when) = reason

						_ <- logger.info (
							s"stopping http service: $why ($when)"
							)

						_ <- IO.sleep (settings.quiescenceDelay.value)
						} yield ()
				}
			.as (ExitCode.Success)
			.handleErrorWith {
				deactivator.signal ("http4s error detected") >>
				LoggerFactory.getLogger[IO]
					.error (_) ("exiting due to http4s error")
					.as (ExitCode.Error)
				}
		}


	/**
	 * The kafka method defines the ability to interact with Kafka distributed
	 * events.
	 */
	private def kafka (
		settings : RuntimeSettings,
		deactivator : ServiceDeactivator[IO]
		)
		(implicit envResource : ReadersWriterResource[IO, GlobalEnvironment[IO]])
		: IO[ExitCode] =
		LoggerFactory.create[IO] >>= {
			implicit logger =>
				val consumers = AllConsumers (settings)

				consumers ().use {
					IO.race (
						_,
						deactivator.await () <* IO.sleep (
							settings.quiescenceDelay.value
							)
						)
						.flatTap {
							case Left (outcome) =>
								val how = outcome.fold (
									"cancelled",
									_.getMessage,
									_ => "completed"
									)

								error"consumer(s) exited: $how"

							case Right ((why, when)) =>
								info"stopping kafka service: $why ($when)"
							}
					}
					.as (ExitCode.Success)
					.handleErrorWith {
						deactivator.signal ("kafka error detected") >>
						logger.error (_) ("exiting due to kafka error")
							.as (ExitCode.Error)
						}
			}


	private def services (
		settings : RuntimeSettings,
		deactivator : ServiceDeactivator[IO]
		)
		: IO[ExitCode] =
		ProvisionEnvironment[IO, ExitCode] (settings) {
			implicit envResource =>
				IO.both (
					kafka (settings, deactivator),
					http (settings, deactivator)
					)
					.flatTap (_ => IO.fromFuture (IO (Kamon.stopModules ())))
					.map {
						case (fromKafka, _) if fromKafka != ExitCode
							.Success =>
							fromKafka

						case (_, fromHttp) =>
							fromHttp
						}
			}
}

