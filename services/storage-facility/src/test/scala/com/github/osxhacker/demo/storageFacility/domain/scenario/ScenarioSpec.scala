package com.github.osxhacker.demo.storageFacility.domain.scenario

import java.util.UUID.randomUUID

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import eu.timepit.refined
import org.scalacheck.Gen
import org.scalatest.{
	FixtureAsyncTestSuite,
	FutureOutcome
	}

import org.scalatest.wordspec.FixtureAsyncWordSpecLike
import org.typelevel.log4cats.noop.NoOpFactory

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.chassis.domain.Slug
import com.github.osxhacker.demo.chassis.domain.event.EventSupport
import com.github.osxhacker.demo.chassis.monitoring.{
	CorrelationId,
	Subsystem
	}

import com.github.osxhacker.demo.chassis.monitoring.logging.ContextualLoggerFactory
import com.github.osxhacker.demo.storageFacility.adapter.database.{
	MockCompany,
	MockStorageFacility
	}
import com.github.osxhacker.demo.storageFacility.domain._
import com.github.osxhacker.demo.storageFacility.domain.event.{
	AllStorageFacilityEvents,
	EventChannel
	}


/**
 * The '''ScenarioSpec''' type provides functionality useful for testing
 * [[com.github.osxhacker.demo.storageFacility.domain.scenario]] definitions.
 * Here, [[cats.effect.IO]] is the container required by
 * [[com.github.osxhacker.demo.storageFacility.domain.ScopedEnvironment]] due to
 * the `implicit`s needed by
 * [[com.github.osxhacker.demo.storageFacility.adapter.database.MockStorageFacility]].
 */
abstract class ScenarioSpec ()
	extends AsyncIOSpec
		with ProjectSpec
		with FixtureAsyncWordSpecLike
		with StorageFacilitySupport
		with EventSupport
{
	/// Self Type Constraints
	this : FixtureAsyncTestSuite =>


	/// Class Imports
	import refined.auto._


	/// Class Types
	final override type FixtureParam = ScopedEnvironment[IO]


	/**
	 * The '''predefined''' `object` contains Domain Object Model collaborators
	 * which will have stable properties.  By having them, they can be used to
	 * pre-populate repositories and be used.
	 */
	protected object predefined
	{
		lazy val facility = StorageFacility.owner
			.replace (tenant) (createFacility ())

		lazy val slug = Slug ("reserved-slug" : Slug.Value)
		lazy val tenant = Company.slug
			.replace (slug) (createArbitrary[Company] ())
	}


	/// Instance Properties
	implicit protected lazy val constDomainSlug = Gen.const (predefined.slug)
	implicit protected val loggerFactory = NoOpFactory[IO]
	implicit protected def subsystem = Subsystem ("unit-test")


	final override def withFixture (test : OneArgAsyncTest) : FutureOutcome =
		withFixture (
			test.toNoArgAsyncTest (createScopedEnvironment ())
			)


	protected def createFacility () : StorageFacility =
		createFacility ("test name")


	protected def createFacility (name : StorageFacility.Name)
		: StorageFacility =
		createFacility (name, createArbitrary[Company] ())


	protected def createFacility (name : StorageFacility.Name, owner : Company)
		: StorageFacility =
		StorageFacility.name
			.replace (name)
			.andThen (
				StorageFacility.owner
					.replace (owner)
				) (createArbitrary[StorageFacility] ())


	/**
	 * This version of the createScopedEnvironment method creates a
	 * [[com.github.osxhacker.demo.storageFacility.domain.ScopedEnvironment]]
	 * with default mock repositories.
	 */
	protected def createGlobalEnvironment () : GlobalEnvironment[IO] =
		createGlobalEnvironment (
			new MockCompany[IO] (predefined.tenant),
			new MockStorageFacility[IO] ()
			)


	/**
	 * This version of the createGlobalEnvironment method creates a
	 * [[com.github.osxhacker.demo.storageFacility.domain.GlobalEnvironment]]
	 * using provided repository instances.
	 */
	protected def createGlobalEnvironment (
		companies : MockCompany[IO],
		storageFacilities : MockStorageFacility[IO]
		)
		: GlobalEnvironment[IO] =
		GlobalEnvironment (
			defaultRegion,
			CompanyReference (predefined.slug),
			companies,
			storageFacilities,
			new MockEventProducer[IO, EventChannel, AllStorageFacilityEvents] (
				EventChannel.UnitTest
				)
			)


	/**
	 * This version of the createGlobalEnvironment method creates a
	 * [[com.github.osxhacker.demo.storageFacility.domain.GlobalEnvironment]]
	 * from an existing
	 * [[com.github.osxhacker.demo.storageFacility.domain.ScopedEnvironment]].
	 */
	protected def createGlobalEnvironment (scoped : ScopedEnvironment[IO])
		: GlobalEnvironment[IO] =
		GlobalEnvironment[IO] (
			scoped.region,
			scoped.tenant,
			scoped.companies,
			scoped.storageFacilities,
			scoped.storageFacilityEvents
			)


	/**
	 * This version of the createScopedEnvironment method creates a
	 * [[com.github.osxhacker.demo.storageFacility.domain.ScopedEnvironment]]
	 * with default mock repositories.
	 */
	protected def createScopedEnvironment () : ScopedEnvironment[IO] =
		createScopedEnvironment (
			new MockCompany[IO] (predefined.tenant),
			new MockStorageFacility[IO] ()
			)


	/**
	 * This version of the createScopedEnvironment method creates a
	 * [[com.github.osxhacker.demo.storageFacility.domain.ScopedEnvironment]]
	 * using provided repository instances.
	 */
	protected def createScopedEnvironment (
		companies : MockCompany[IO],
		storageFacilities : MockStorageFacility[IO]
		)
		: ScopedEnvironment[IO] =
		ScopedEnvironment[IO] (
			defaultRegion,
			CompanyReference (predefined.slug),
			companies,
			storageFacilities,
			new MockEventProducer[IO, EventChannel, AllStorageFacilityEvents] (
				EventChannel.UnitTest
				)
			) (
			CorrelationId (randomUUID),
			ContextualLoggerFactory[IO] (NoOpFactory[IO]) (Map.empty)
			)
}

