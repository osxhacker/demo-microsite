package com.github.osxhacker.demo.company.domain.scenario

import java.util.UUID.randomUUID

import scala.collection.mutable

import cats.data.NonEmptySet
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import eu.timepit.refined
import org.scalatest.{
	Assertion,
	FutureOutcome
	}

import org.scalatest.wordspec.FixtureAsyncWordSpecLike
import org.typelevel.log4cats.noop.NoOpFactory
import shapeless.Coproduct

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.chassis.domain.{
	ErrorOr,
	Slug
	}

import com.github.osxhacker.demo.chassis.domain.algorithm.GenerateServiceFingerprint
import com.github.osxhacker.demo.chassis.domain.event.{
	EventProducer,
	EventSupport,
	Region
	}

import com.github.osxhacker.demo.chassis.monitoring.{
	CorrelationId,
	Subsystem
	}

import com.github.osxhacker.demo.company.adapter.database.InMemoryCompanyRepository
import com.github.osxhacker.demo.company.domain.{
	CompanySupport,
	GlobalEnvironment,
	ScopedEnvironment
	}

import com.github.osxhacker.demo.company.domain.event.{
	AllCompanyEvents,
	EventChannel
	}


/**
 * The '''ScenarioSpec''' type provides functionality useful for testing
 * [[com.github.osxhacker.demo.company.domain.scenario]] definitions.
 * Here, [[cats.effect.IO]] is the container required by
 * [[com.github.osxhacker.demo.company.domain.ScopedEnvironment]] due to
 * `withFixture` having to return a [[org.scalatest.FutureOutcome]].  Since any
 * synchronous test can be represented by a [[org.scalatest.FutureOutcome]], it
 * is chosen.
 */
abstract class ScenarioSpec ()
	extends AsyncIOSpec
		with ProjectSpec
		with FixtureAsyncWordSpecLike
		with CompanySupport
		with EventSupport
{
	/// Class Imports
	import mouse.any._
	import refined.auto._


	/// Class Types
	override type FixtureParam = ScopedEnvironment[IO]


	/// Instance Properties
	implicit protected val loggerFactory = NoOpFactory[IO]
	implicit protected def subsystem = Subsystem ("unit-test")


	final override def withFixture (test : OneArgAsyncTest) : FutureOutcome =
		ScopedEnvironment[IO] (
			createGlobalEnvironment (),
			CorrelationId[ErrorOr] (randomUUID ()).orThrow ()
			) |>
			test.toNoArgAsyncTest |>
			super.withFixture


	/**
	 * The createGlobalEnvironment method creates a
	 * [[com.github.osxhacker.demo.company.domain.GlobalEnvironment]]
	 * with an
	 * [[com.github.osxhacker.demo.company.adapter.database.InMemoryCompanyRepository]].
	 * If alternate collaborators are needed for a unit-test, `override` this
	 * method.
	 */
	protected def createGlobalEnvironment () : GlobalEnvironment[IO] =
		GlobalEnvironment[IO] (
			Region ("unit-test"),
			GenerateServiceFingerprint[ErrorOr] ().orThrow (),
			NonEmptySet.one (Slug ("test-company")),
			InMemoryCompanyRepository[IO] (),
			createEventProducer ()
			)


	/**
	 * The createEventProducer method creates a
	 * [[com.github.osxhacker.demo.chassis.domain.event.EventSupport.MockEventProducer]]
	 * suitable for fulfilling the
	 * [[com.github.osxhacker.demo.chassis.domain.event.EventProducer]] contract
	 * in unit tests.  If an alternate
	 * [[com.github.osxhacker.demo.chassis.domain.event.EventProducer]] is
	 * desired, `override` this method.
	 */
	protected def createEventProducer ()
		: MockEventProducer[IO, EventChannel, AllCompanyEvents] =
		new MockEventProducer[IO, EventChannel, AllCompanyEvents] (
			EventChannel.UnitTest
			)


	protected def inspectEvents[DomainEventsT <: Coproduct] (
		producer : EventProducer[IO, EventChannel, DomainEventsT]
		)
		(f : mutable.ListBuffer[DomainEventsT] => Assertion)
		: Assertion =
		producer match {
			case MockEventProducer (_, emitted) =>
				f (emitted)

			case other =>
				fail (s"producer was not a MockEventProducer: $other")
			}
}

