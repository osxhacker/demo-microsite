package $package$.adapter

import scala.annotation.unused
import scala.concurrent.duration._
import scala.language.postfixOps

import cats.Monad
import cats.effect._
import eu.timepit.refined
import org.http4s.blaze.server.BlazeServerBuilder
import org.typelevel.log4cats
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

import com.github.osxhacker.demo.chassis.adapter.{
	AbstractServer,
	ProgramArguments,
	ServiceDeactivator
	}


/**
 * The '''Server''' `object` defines the entrypoint for the $name$
 * process.
 */
object Server
	extends AbstractServer (
		name = "$name$",
		description = "$description$"
		)
		with ProgramArguments
{
	/// Class Imports
	import ProgramArguments.OperatingMode
	import cats.syntax.flatMap._
	import log4cats.syntax._
	import refined.auto._


	/// Instance Properties
	implicit override protected val loggerFactory : LoggerFactory[IO] =
		Slf4jFactory.create[IO]


	override def main (mode : OperatingMode) : IO[ExitCode] =
		Monad[IO].flatMap2 (
			RuntimeSettings[IO] (mode.settings.value),
			ServiceDeactivator[IO] ()
			) (services)


	/**
	 * The http method defines the ability to expose RESTful resources as
	 * defined by the service's RAML API.
	 */
	private def http (
		settings : RuntimeSettings,
		deactivator : ServiceDeactivator[IO]
		)
		: IO[ExitCode] =
		ProvisionEnvironment[IO, ExitCode] (settings) {
			implicit envResource =>
				val allResources = rest.AllResources[IO] (
					settings,
					deactivator
					)

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
									s"starting http service uri=\${server.baseUri}"
									)

								reason <- deactivator.await ()

								(why, when) = reason

								_ <- logger.info (
									s"stopping http service: \$why (\$when)"
									)

								_ <- IO.sleep (1 second)
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
	 *
	 * TODO: define and implement event processing logic
	 */
	private def kafka (
		@unused settings : RuntimeSettings,
		deactivator : ServiceDeactivator[IO]
		)
		: IO[ExitCode] =
		LoggerFactory.create[IO] >>= {
			implicit logger =>
				deactivator.await ()
					.flatTap {
						case (why, when) =>
							info"stopping kafka service: \$why (\$when)"
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
		IO.both (kafka (settings, deactivator), http (settings, deactivator))
			.map {
				case (fromKafka, _) if fromKafka != ExitCode.Success =>
					fromKafka

				case (_, fromHttp) =>
					fromHttp
				}
}

