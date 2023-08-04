package $package$.adapter.rest

import scala.language.postfixOps

import cats.effect._
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.{
	FixtureAsyncTestSuite,
	FutureOutcome
	}

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.chassis.adapter.rest.TapirClientSupport
import com.github.osxhacker.demo.chassis.effect.ReadersWriterResource
import com.github.osxhacker.demo.chassis.monitoring.Subsystem
import $package$.adapter.RuntimeSettings
import $package$.domain._


/**
 * The '''ResourceSpec''' type provides functionality useful for testing
 * [[$package$.adapter.rest]] resource
 * definitions with [[sttp.client3]] and
 * [[$package$.adapter]] concepts.
 */
abstract class ResourceSpec ()
	extends ProjectSpec
		with AsyncIOSpec
		with TapirClientSupport
		with $aggregate$Support
{
	/// Self Type Constraints
	this : FixtureAsyncTestSuite =>


	/// Class Types
	final override type FixtureParam = ReadersWriterResource[
		IO,
		GlobalEnvironment[IO]
		]


	/// Instance Properties
	implicit protected val subsystem = Subsystem ("resource-unit-test")
	protected lazy val defaultSettings =
		RuntimeSettings[Either[Throwable, *]] ().orFail ()

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


	/**
	 * The createEnvironmentResource method is a model of the FACTORY pattern
	 * and will mint a
	 * [[com.github.osxhacker.demo.chassis.effect.ReadersWriterResource]] having
	 * a [[$package$.domain.GlobalEnvironment]]
	 * with collaborators "stubbed" as needed.
	 */
	protected def createEnvironmentResource ()
		: IO[ReadersWriterResource[IO, GlobalEnvironment[IO]]] =
		ReadersWriterResource.from (GlobalEnvironment[IO] ())


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

