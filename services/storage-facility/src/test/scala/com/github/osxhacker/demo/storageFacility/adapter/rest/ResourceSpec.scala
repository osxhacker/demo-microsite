package com.github.osxhacker.demo.storageFacility.adapter.rest

import scala.language.postfixOps

import cats.effect._
import cats.effect.testing.scalatest.AsyncIOSpec
import eu.timepit.refined
import org.scalatest.{
	FixtureAsyncTestSuite,
	FutureOutcome
	}

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.chassis.domain.event.{
	EventSupport,
	Region
	}

import com.github.osxhacker.demo.chassis.adapter.rest.TapirClientSupport
import com.github.osxhacker.demo.chassis.domain.Slug
import com.github.osxhacker.demo.chassis.domain.repository.CreateIntent
import com.github.osxhacker.demo.chassis.effect.ReadersWriterResource
import com.github.osxhacker.demo.chassis.monitoring.Subsystem
import com.github.osxhacker.demo.storageFacility.adapter.RuntimeSettings
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
 * The '''ResourceSpec''' type provides functionality useful for testing
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest]] resource
 * definitions with [[sttp.client3]] and
 * [[com.github.osxhacker.demo.storageFacility.adapter]] concepts.
 */
abstract class ResourceSpec ()
	extends ProjectSpec
		with AsyncIOSpec
		with TapirClientSupport
		with ApiSupport
		with StorageFacilitySupport
		with EventSupport
{
	/// Self Type Constraints
	this : FixtureAsyncTestSuite =>


	/// Class Imports
	import cats.syntax.show._
	import cats.syntax.traverse._
	import refined.auto._
	import sttp.client3.UriContext


	/// Class Types
	final override type FixtureParam = ReadersWriterResource[
		IO,
		GlobalEnvironment[IO]
		]


	/**
	 * The '''predefined''' `object` contains Domain Object Model collaborators
	 * which will have stable properties.  By having them, they can be used to
	 * pre-populate repositories and be part of URI path definitions.
	 */
	protected object predefined
	{
		val slug = Slug ("example" : Slug.Value)
		val tenant = Company.slug
			.replace (slug) (createArbitrary[Company] ())
	}


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

	protected lazy val tenantRoot = uri"$apiRoot/${predefined.slug.show}"


	def withFixture (test : OneArgAsyncTest) : FutureOutcome =
		new FutureOutcome (
			createEnvironmentResource ()
				.unsafeToFuture ()
				.flatMap {
					env =>
						withFixture (test.toNoArgAsyncTest (env))
							.toFuture
					}
			)


	protected def addFacilities (count : Int, owner : Company)
		(implicit guardedEnv : FixtureParam)
		: IO[List[StorageFacility]] =
		guardedEnv.reader {
			env =>
				0.until (count)
					.toList
					.traverse {
						_ =>
							env.storageFacilities.save (
								CreateIntent (
									StorageFacility.owner
										.replace (owner) (
											createArbitrary[StorageFacility] ()
											)
									)
								)
								.map (_.orFail ("unable to save facility"))
						}
			}


	/**
	 * The createEnvironmentResource method is a model of the FACTORY pattern
	 * and will mint a
	 * [[com.github.osxhacker.demo.chassis.effect.ReadersWriterResource]] having
	 * an [[com.github.osxhacker.demo.storageFacility.domain.GlobalEnvironment]]
	 * with collaborators "stubbed" as needed.
	 */
	protected def createEnvironmentResource ()
		: IO[ReadersWriterResource[IO, GlobalEnvironment[IO]]] =
	{
		ReadersWriterResource.from (
			GlobalEnvironment[IO] (
				Region ("unit-testing"),
				predefined.tenant.toRef (),
				new MockCompany[IO] (predefined.tenant),
				new MockStorageFacility[IO] (),
				new MockEventProducer[IO, EventChannel, AllStorageFacilityEvents] (
					EventChannel.UnitTest
					)
				)
			)
	}


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

