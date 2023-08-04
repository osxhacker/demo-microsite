package com.github.osxhacker.demo.company.adapter.rest

import scala.language.postfixOps

import cats.data.NonEmptySet
import cats.effect._
import cats.effect.testing.scalatest.AsyncIOSpec
import eu.timepit.refined
import eu.timepit.refined.types.string.NonEmptyString
import org.scalatest.{
	FixtureAsyncTestSuite,
	FutureOutcome
	}

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.chassis.adapter.rest.TapirClientSupport
import com.github.osxhacker.demo.chassis.domain.{
	ErrorOr,
	Slug
	}

import com.github.osxhacker.demo.chassis.domain.algorithm.GenerateServiceFingerprint
import com.github.osxhacker.demo.chassis.domain.event.{
	EventSupport,
	Region
	}

import com.github.osxhacker.demo.chassis.effect.ReadersWriterResource
import com.github.osxhacker.demo.chassis.monitoring.Subsystem
import com.github.osxhacker.demo.chassis.domain.repository.CreateIntent
import com.github.osxhacker.demo.company.adapter.RuntimeSettings
import com.github.osxhacker.demo.company.adapter.database.InMemoryCompanyRepository
import com.github.osxhacker.demo.company.domain._
import com.github.osxhacker.demo.company.domain.event.{
	AllCompanyEvents,
	EventChannel
	}


/**
 * The '''ResourceSpec''' type provides functionality useful for testing
 * [[com.github.osxhacker.demo.company.adapter.rest]] resource
 * definitions with [[sttp.client3]] and
 * [[com.github.osxhacker.demo.company.adapter]] concepts.
 */
abstract class ResourceSpec ()
	extends ProjectSpec
		with AsyncIOSpec
		with TapirClientSupport
		with ApiSupport
		with CompanySupport
		with EventSupport
{
	/// Self Type Constraints
	this : FixtureAsyncTestSuite =>


	/// Class Imports
	import cats.syntax.traverse._
	import refined.auto._


	/// Class Types
	final override type FixtureParam = ReadersWriterResource[
		IO,
		GlobalEnvironment[IO]
		]


	/// Instance Properties
	implicit protected val subsystem = Subsystem ("resource-unit-test")
	protected lazy val defaultSettings = RuntimeSettings[ErrorOr] ().orFail ()
	protected lazy val apiRoot = "http://%s:%d%s".format (
		defaultSettings.http.address.value,
		defaultSettings.http.port.value,
		defaultSettings.http.api.value
		)

	protected lazy val internalRoot = "http://%s:%d%s/internal".format (
		defaultSettings.http.address.value,
		defaultSettings.http.port.value,
		defaultSettings.http.api.value
		)


	override def withFixture (test : OneArgAsyncTest) : FutureOutcome =
		new FutureOutcome (
			createEnvironmentResource ()
				.unsafeToFuture ()
				.flatMap {
					env =>
						withFixture (test.toNoArgAsyncTest (env))
							.toFuture
					}
			)


	protected def addCompanies (count : Int)
		(implicit guardedEnv : FixtureParam)
		: IO[List[Company]] =
		guardedEnv.reader {
			env =>
				0.until (count)
					.toList
					.traverse {
						_ =>
							env.companies
								.save (CreateIntent (createArbitrary[Company] ())
								)
								.map (_.orFail ("unable to save company"))
					}
		}


	/**
	 * The createEnvironmentResource method is a model of the FACTORY pattern
	 * and will mint a
	 * [[com.github.osxhacker.demo.chassis.effect.ReadersWriterResource]] having
	 * a [[com.github.osxhacker.demo.company.domain.GlobalEnvironment]]
	 * with collaborators "stubbed" as needed.
	 */
	protected def createEnvironmentResource ()
		: IO[ReadersWriterResource[IO, GlobalEnvironment[IO]]] =
		for {
			region <- Region[IO] (NonEmptyString ("test"))
			fingerprint <- GenerateServiceFingerprint[IO] ()
			resource <- ReadersWriterResource.from[IO, GlobalEnvironment[IO]] (
				GlobalEnvironment[IO] (
					region,
					fingerprint,
					NonEmptySet.one (Slug ("test-company")),
					InMemoryCompanyRepository[IO] (),
					new MockEventProducer[IO, EventChannel, AllCompanyEvents] (
						EventChannel.UnitTest
						)
					)
				)
			} yield resource


	/**
	 * The sequence method evaluates the '''first''' __and then__ '''second'''
	 * [[cats.effect.IO]] instances in order, producing a tuple of the results.
	 */
	protected def sequence[A, B] (first : IO[A], second : IO[B]) : IO[(A, B)] =
		first flatMap {
			a =>
				second map (a -> _)
			}
}

