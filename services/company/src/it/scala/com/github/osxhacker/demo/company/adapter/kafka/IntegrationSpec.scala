package com.github.osxhacker.demo.company.adapter.kafka

import java.util.UUID.randomUUID

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import eu.timepit.refined
import fs2.kafka._
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AsyncWordSpecLike
import org.typelevel.log4cats.noop.NoOpFactory

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.chassis.domain.entity.{
	Identifier,
	ModificationTimes,
	Version
	}

import com.github.osxhacker.demo.chassis.domain.Slug
import com.github.osxhacker.demo.chassis.domain.event.EmitEvents
import com.github.osxhacker.demo.chassis.monitoring.Subsystem
import com.github.osxhacker.demo.company.adapter.{
	ProvisionEnvironment,
	RuntimeSettings
	}

import com.github.osxhacker.demo.company.domain._
import com.github.osxhacker.demo.company.domain.event.AllCompanyEvents


/**
 * The '''IntegrationSpec''' type defines common behaviour for implementing
 * integration [[com.github.osxhacker.demo.company.adapter.kafka]] test suites.
 * Each concrete specification is expected to provide a `configuration` resource
 * specific to it.  Furthermore, Kafka topics are recreated for __each__ test
 * block.
 */
abstract class IntegrationSpec (protected val resource : IntegrationSettings)
	extends AsyncIOSpec
		with AsyncWordSpecLike
		with Diagrams
		with ProjectSpec
		with EmitEvents[ScopedEnvironment[IO], AllCompanyEvents]
{
	/// Class Imports
	import cats.syntax.applicative._
	import refined.auto._


	/// Instance Properties
	implicit protected val noopFactory = NoOpFactory[IO]
	implicit protected val subsystem = Subsystem ("karaf")


	protected def createCompany () : Company =
		Company (
			id = Identifier.fromRandom[Company] (),
			version = Version.initial,
			slug = Slug ("test-company"),
			name = "Acme Test Co.",
			status = CompanyStatus.Active,
			description = "This is a test",
			timestamps = ModificationTimes.now ()
			)


	protected def withGlobalEnvironment[A] (
		testCode : (RuntimeSettings, GlobalEnvironment[IO]) => IO[A]
		)
		: IO[A] =
		for {
			settings <- RuntimeSettings[IO] (resource.configuration)
			_ <- deleteTopic (
				settings.kafka.servers.value,
				settings.kafka.company.topic.map (_.value)
				)

			global <- ProvisionEnvironment[IO, GlobalEnvironment[IO]] (settings) (
				_.reader (_.pure[IO])
				)

			result <- testCode (settings, global)
			} yield result


	protected def withScopedEnvironment[A] (
		testCode : ScopedEnvironment[IO] => IO[A]
		)
		: IO[A] =
		for {
			settings <- RuntimeSettings[IO] (resource.configuration)
			_ <- deleteTopic (
				settings.kafka.servers.value,
				settings.kafka.company.topic.map (_.value)
				)

			result <- ProvisionEnvironment[IO, A] (settings) (
				_.reader {
					_.scopeWith (randomUUID ().toString) flatMap testCode
					}
				)
			} yield result


	private def deleteTopic (servers : String, topic : Option[String])
		: IO[Unit] =
		topic.fold (IO.unit) {
			name =>
				KafkaAdminClient.resource[IO] (AdminClientSettings (servers))
					.use {
						_.deleteTopic (name)
							.recoverWith (_ => IO.unit)
						}
			}
}

