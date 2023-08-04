package com.github.osxhacker.demo.chassis.adapter

import scala.concurrent.duration._
import scala.language.postfixOps

import cats.ApplicativeThrow
import eu.timepit.refined
import eu.timepit.refined.api.{
	Refined,
	Validate
	}

import pureconfig.ConfigSource

import com.github.osxhacker.demo.chassis.domain.event.Channel


/**
 * The '''RuntimeSettingsCompanion''' type defines common
 * [[eu.timepit.refined]] types available for service project
 * ''SettingsT'' definitions.
 */
trait RuntimeSettingsCompanion[SettingsT <: Product]
{
	/// Class Imports
	import refined.boolean._
	import refined.collection._
	import refined.generic.Equal
	import refined.numeric.Interval
	import refined.string._


	/// Class Types
	final type IdleTimeout = Refined[
		FiniteDuration,
		AllowedIdleTimeout
		]


	type KafkaServers = Refined[
		String,
		Trimmed And
			NonEmpty And
			MatchesRegex[
				"^[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*:[0-9]{4,5}(?:,[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*:[0-9]{4,5})*$"
				]
		]


	final type NetworkAddress = Refined[
		String,
		Trimmed And
			NonEmpty And
			Or[
				IPv4 Or
				IPv6 Or
				Equal["localhost"],
				MatchesRegex["^[A-Za-z0-9](?:[A-Za-z0-9.])*$"]
				]
		]


	final type NetworkPort = Refined[
		Int,
		Interval.Open[1024, 32768]
		]


	final type Path = Refined[
		String,
		rest.Path.PredicateType
		]


	final type QuiescenceDelay = Refined[
		FiniteDuration,
		AllowedQuiescenceDelay
		]


	final type Region = Refined[
		String,
		MatchesRegex["^[a-z][a-z0-9]*(?:-[a-z0-9]+)*(?:-(?:dev|qa|prod|stage|[0-9]+))?$"]
		]


	/**
	 * The '''AllowedIdleTimeout''' type is used for defining a custom
	 * [[eu.timepit.refined.api.Refined]] predicate to validate idle timeouts.
	 *
	 * @see https://github.com/fthomas/refined/blob/master/modules/docs/custom_predicates.md
	 */
	final case class AllowedIdleTimeout ()


	/**
	 * The '''AllowedQuiescenceDelay''' type is used for defining a custom
	 * [[eu.timepit.refined.api.Refined]] predicate to validate quiescence
	 * delays.
	 *
	 * @see https://github.com/fthomas/refined/blob/master/modules/docs/custom_predicates.md
	 */
	final case class AllowedQuiescenceDelay ()


	/**
	 * The '''KarafChannel''' type reifies the configurable parameters (knobs)
	 * relating to a specific Karaf channel (topic).
	 */
	final case class KarafChannel (
		val topic : Option[KarafChannel.Topic],
		val numberOfPartitions : KarafChannel.Partitions,
		val replicationFactor : KarafChannel.ReplicationFactor
		)
	{
		/**
		 * The topicNameOrDefault method resolves the Kafka `topic` name if
		 * present in the settings or uses the given '''channel''' `entryName`.
		 */
		def topicNameOrDefault (channel : Channel) : String =
			topic.fold (channel.entryName) (_.value)
	}


	object KarafChannel
	{
		/// Class Types
		type Partitions = Refined[
			Int,
			Interval.Closed[1, 20]
			]


		type ReplicationFactor = Refined[
			Short,
			Interval.Closed[1, 5]
			]


		type Topic = Refined[
			String,
			Trimmed And
				NonEmpty And
				MatchesRegex["^[a-z][a-z0-9.]*(?:-[a-z][a-z0-9.]*)*$"]
			]
	}


	/**
	 * This version of the apply method attempts to load a '''SettingsT''' from
	 * the given [[pureconfig.ConfigSource]].
	 */
	def apply[F[_]] (source : ConfigSource)
		(implicit AT : ApplicativeThrow[F])
		: F[SettingsT]


	/**
	 * This version of the apply method attempts to load a '''RuntimeSettings'''
	 * from a candidate resource '''name'''.
	 */
	final def apply[F[_]] (name : String)
		(implicit AT : ApplicativeThrow[F])
		: F[SettingsT] =
		apply[F] (ConfigSource.resources (name))


	/**
	 * This version of the apply method attempts to load a '''SettingsT''' from
	 * the `default` [[pureconfig.ConfigSource]].
	 */
	final def apply[F[_]] ()
		(implicit AT : ApplicativeThrow[F])
		: F[SettingsT] =
		apply[F] (ConfigSource.default)


	/// Implicit Conversions
	implicit val validateAllowedIdleTimeout
		: Validate.Plain[FiniteDuration, AllowedIdleTimeout] =
		Validate.fromPredicate (
			duration => duration >= 10.seconds && duration <= 5.minutes,
			valid => s"($valid is between 10 seconds and 5 minutes)",
			AllowedIdleTimeout ()
			)


	implicit val validateAllowedQuiescenceDelay
		: Validate.Plain[FiniteDuration, AllowedQuiescenceDelay] =
		Validate.fromPredicate (
			duration => duration >= 1.second && duration <= 30.seconds,
			valid => s"($valid is between 1 and 30 seconds)",
			AllowedQuiescenceDelay ()
			)
}

