package $package$.adapter

import scala.language.postfixOps

import cats.ApplicativeThrow
import eu.timepit.refined
import eu.timepit.refined.pureconfig._
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._

import com.github.osxhacker.demo.chassis.adapter.RuntimeSettingsCompanion


/**
 * The '''RuntimeSettings''' type defines the server parameters available for
 * tuning the [[$package$.adapter.Server]].
 *
 * ==Knobs==
 *
 *   - '''idle-timeout''': Server request idle time before closeing a
 *     connection.
 *
 *   - '''http.address''': ''String'' having the machine name for `bind`ing.
 *
 *   - '''http.api''': ''String'' containing the [[java.net.URI]] path prefix
 *     for __all__ API endpoints.
 *
 *   - '''http.port''': Non-privileged port number for `bind`ing.
 */
final case class RuntimeSettings (
	val idleTimeout : RuntimeSettings.IdleTimeout,
	val http : RuntimeSettings.Http
	)


object RuntimeSettings
	extends RuntimeSettingsCompanion[RuntimeSettings]
{
	/// Class Imports
	import cats.syntax.either._


	/// Class Types
	/**
	 * The '''Http''' type reifies the configurable parameters (knobs) relating
	 * to exposing an HTTP API.
	 */
	final case class Http (
		val address : NetworkAddress,
		val api : Path,
		val port : NetworkPort
		)


	/**
	 * This version of the apply method attempts to load a '''RuntimeSettings'''
	 * from the given [[pureconfig.ConfigSource]].
	 */
	override def apply[F[_]] (source : ConfigSource)
		(implicit MT : ApplicativeThrow[F])
		: F[RuntimeSettings] =
		source.load[RuntimeSettings]
			.leftMap (ConfigReaderException[RuntimeSettings])
			.liftTo[F]
}

