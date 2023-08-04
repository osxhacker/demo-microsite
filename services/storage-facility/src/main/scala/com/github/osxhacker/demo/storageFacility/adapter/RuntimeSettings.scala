package com.github.osxhacker.demo.storageFacility.adapter

import scala.language.postfixOps

import cats.ApplicativeThrow
import eu.timepit.refined
import eu.timepit.refined.pureconfig._
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._

import com.github.osxhacker.demo.chassis.adapter.RuntimeSettingsCompanion
import com.github.osxhacker.demo.chassis.domain.Slug


/**
 * The '''RuntimeSettings''' type defines the server parameters available for
 * tuning the [[com.github.osxhacker.demo.storageFacility.adapter.Server]].
 *
 * ==Knobs==
 *
 *   - '''database.driver''': ''String'' having the fully qualified JDBC
 *     driver [[java.lang.Class]] name.
 *
 *   - '''database.password''': ''String'' containing the password to use when
 *     connecting with the '''database.url'''.
 *
 *   - '''database.url''': ''String'' containing the connection
 *      [[java.net.URI]].
 *
 *   - '''database.user''': ''String'' containing the account name to use when
 *      connecting with the '''database.url'''.
 *
 *   - '''http.address''': ''String'' having the machine name for `bind`ing.
 *
 *   - '''http.api''': ''String'' containing the [[java.net.URI]] path prefix
 *     for __all__ API endpoints.
 *
 *   - '''http.port''': Non-privileged port number for `bind`ing.
 *
 *   - '''idle-timeout''': Server request idle time before closing the
 *     connection.
 *
 *   - '''kafka.company''': Kafka configuration for the
 *     [[com.github.osxhacker.demo.storageFacility.domain.event.EventChannel.Company]]
 *     channel (consumer only).
 *
 *   - '''kafka.storageFacility''': Kafka configuration for the
 *     [[com.github.osxhacker.demo.storageFacility.domain.event.EventChannel.StorageFacility]]
 *     channel (consumer and producer).
 *
 *   - '''operations-slug''': [[com.github.osxhacker.demo.chassis.domain.Slug]]
 *     identifying the DevOps
 *     [[com.github.osxhacker.demo.storageFacility.domain.Company]] used for
 *     internal operations.
 *
 *   - '''quiescence-delay''': How long to wait before stopping the service.
 *
 *   - '''region''': The deployment region for all instances of the
 *     microservice.
 */
final case class RuntimeSettings (
	val idleTimeout : RuntimeSettings.IdleTimeout,
	val operationsSlug : Slug.Value,
	val quiescenceDelay : RuntimeSettings.QuiescenceDelay,
	val database : RuntimeSettings.Database,
	val http : RuntimeSettings.Http,
	val kafka : RuntimeSettings.Kafka,
	val region : RuntimeSettings.Region
	)


object RuntimeSettings
	extends RuntimeSettingsCompanion[RuntimeSettings]
{
	/// Class Imports
	import cats.syntax.either._


	/// Class Types
	/**
	 * The '''Database''' type reifies the configurable parameters (knobs)
	 * relating to using a persistent store.
	 */
	final case class Database (
		val driver : Database.Driver,
		val url : Database.ConnectionUrl,
		val user : Database.User,
		val password : Database.Password
		)


	object Database
	{
		/// Class Imports
		import refined.api.Refined
		import refined.boolean.And
		import refined.collection.NonEmpty
		import refined.string._


		/// Class Types
		type ConnectionUrl = Refined[
			String,
			Trimmed And Uri
			]


		type Driver = Refined[
			String,
			Trimmed And NonEmpty
			]


		type Password = Refined[
			String,
			Trimmed
			]


		type User = Refined[
			String,
			Trimmed
			]
	}


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
		val company : KarafChannel,
		val storageFacility : KarafChannel
		)


	/**
	 * This version of the apply method attempts to load a '''RuntimeSettings'''
	 * from the given [[pureconfig.ConfigSource]].
	 */
	override def apply[F[_]] (source : ConfigSource)
		(implicit applicativeThrow : ApplicativeThrow[F])
		: F[RuntimeSettings] =
		source.load[RuntimeSettings]
			.leftMap (ConfigReaderException[RuntimeSettings])
			.liftTo[F]
}

