package com.github.osxhacker.demo.storageFacility.adapter.database

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import fs2.kafka._
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AsyncWordSpecLike
import org.typelevel.log4cats.noop.NoOpFactory

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.storageFacility.adapter.{
	ProvisionEnvironment,
	RuntimeSettings
	}

import com.github.osxhacker.demo.storageFacility.domain.GlobalEnvironment


/**
 * The '''IntegrationSpec''' type defines the common super-type for __all__
 * [[com.github.osxhacker.demo.storageFacility.adapter.database]] integration
 * specifications.
 */
abstract class IntegrationSpec (private val resource : IntegrationSettings)
	extends AsyncIOSpec
		with AsyncWordSpecLike
		with Diagrams
		with ProjectSpec
{
	/// Class Imports
	import cats.syntax.all._


	/// Instance Properties
	implicit protected val noopFactory = NoOpFactory[IO]


	/**
	 * The withGlobalEnvironment method uses the
	 * [[com.github.osxhacker.demo.storageFacility.adapter.RuntimeSettings]]
	 * identified by `resource` to drive
	 * [[com.github.osxhacker.demo.storageFacility.adapter.ProvisionEnvironment]],
	 * creating the requisite
	 * [[com.github.osxhacker.demo.storageFacility.domain.GlobalEnvironment]] to
	 * provide to the given '''testCode'''.
	 * Each invocation ensures there are no Kafka topics corresponding to
	 * known
	 * [[com.github.osxhacker.demo.storageFacility.domain.event.EventChannel]]s.
	 */
	protected def withGlobalEnvironment[A] (
		testCode : GlobalEnvironment[IO] => IO[A]
		) : IO[A] =
		for {
			settings <- RuntimeSettings[IO] (resource.configuration)
			_ <- deleteTopics (settings.kafka.servers.value) {
				settings.kafka.company.topic.map (_.value) ::
				settings.kafka.storageFacility.topic.map (_.value) ::
				Nil
				}

			result <- ProvisionEnvironment[IO, A] (settings) {
				_.reader {
					testCode (_)
					}
				}
			} yield result


	private def deleteTopics (servers : String)
		(topics : Seq[Option[String]])
		: IO[Unit] =
		topics.flatten.traverse {
			name =>
				KafkaAdminClient.resource[IO] (AdminClientSettings (servers))
					.use {
						_.deleteTopic (name)
							.recoverWith (_ => IO.unit)
					}
			}
			.void
}

