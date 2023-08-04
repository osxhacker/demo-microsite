package com.github.osxhacker.demo.company.adapter

import scala.language.postfixOps

import cats.ApplicativeThrow
import cats.data.NonEmptyList
import eu.timepit.refined
import eu.timepit.refined.pureconfig._
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._
import pureconfig.module.cats._

import com.github.osxhacker.demo.chassis.adapter.RuntimeSettingsCompanion
import com.github.osxhacker.demo.chassis.domain.Slug


/**
 * The '''RuntimeSettings''' type defines the server parameters available for
 * tuning the [[com.github.osxhacker.demo.company.adapter.Server]].
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
 *
 *   - '''quiescence-delay''': How long to wait before stopping the service.
 *
 *   - '''region''': The deployment region for all instances of the
 *     microservice.
 *
 *   - '''reserved-slugs''': A [[cats.data.NonEmptyList]] of
 *     [[com.github.osxhacker.demo.chassis.domain.Slug]]s which are not allowed
 *     to be used by external actors.
 */
final case class RuntimeSettings (
	val idleTimeout : RuntimeSettings.IdleTimeout,
	val quiescenceDelay : RuntimeSettings.QuiescenceDelay,
	val http : RuntimeSettings.Http,
	val kafka : RuntimeSettings.Kafka,
	val region : RuntimeSettings.Region,
	val reservedSlugs : NonEmptyList[Slug.Value]
	)


object RuntimeSettings
	extends RuntimeSettingsCompanion[RuntimeSettings]
{
	/// Class Imports
	import cats.syntax.either._
	import refined.api.Refined
	import refined.numeric.Interval


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
	 * The '''Kafka''' type reifies the configurable parameters (knobs) relating
	 * to interacting with one or more topics on Kafka servers.
	 */
	final case class Kafka (
		val servers : KafkaServers,
		val company : KarafChannel
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

