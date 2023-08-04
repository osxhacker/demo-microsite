package com.github.osxhacker.demo.company.domain

import java.time.Instant
import java.util.UUID.randomUUID

import cats.data.NonEmptySet
import eu.timepit.refined
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec
import org.typelevel.log4cats.noop.NoOpFactory
import shapeless.CNil

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.chassis.domain.{
	ErrorOr,
	Slug
	}

import com.github.osxhacker.demo.chassis.domain.algorithm.GenerateServiceFingerprint
import com.github.osxhacker.demo.chassis.domain.entity._
import com.github.osxhacker.demo.chassis.domain.event.{
	EventSupport,
	Region
	}

import com.github.osxhacker.demo.chassis.monitoring.{
	CorrelationId,
	Subsystem
	}

import com.github.osxhacker.demo.company.adapter.database.AlwaysFailCompanyRepository

import event.{
	AllCompanyEvents,
	CompanyChangeEvents,
	EventChannel
	}

import specification.CompanyStatusCanBecome


/**
 * The '''CompanySpec ''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.company.domain.Company]] for
 * fitness of purpose and serves as an exemplar of its use.
 */
final class CompanySpec ()
	extends AnyWordSpec
		with Diagrams
		with ScalaCheckPropertyChecks
		with ProjectSpec
		with CompanySupport
		with EventSupport
{
	/// Class Imports
	import refined.auto._


	/// Instance Properties
	implicit private val loggerFactory = NoOpFactory[ErrorOr]
	implicit private val subsystem = Subsystem ("unit-test")
	implicit private val env = GlobalEnvironment[ErrorOr] (
		Region ("unit-test"),
		GenerateServiceFingerprint[ErrorOr] ().orThrow (),
		NonEmptySet.one (Slug ("test-company")),
		AlwaysFailCompanyRepository[ErrorOr] (
			new RuntimeException ("should never have been called")
			),

		new MockEventProducer[ErrorOr, EventChannel, AllCompanyEvents] (
			EventChannel.UnitTest
			)
		)
		.scopeWith (
			CorrelationId[ErrorOr] (randomUUID ()).orThrow ()
			)
		.orThrow ()

	private val epoch = ModificationTimes (
		createdOn = Instant.ofEpochSecond (0L),
		lastChanged = Instant.ofEpochSecond (0L)
		)


	"The Company entity" must {
		"define equality by id and version alone" in {
			forAll {
				instance : Company =>
					val changed = Company.name
						.replace ("A Different Name") (instance)

					assert (instance === changed)
				}
			}

		"define its hash code by id and version alone" in {
			forAll {
				instance : Company =>
					val changed = Company.name
						.replace ("A Different Name") (instance)

					assert (instance.hashCode () === instance.hashCode ())
					assert (instance.hashCode () === changed.hashCode ())
				}
			}

		"be able to detect changes other than modification times" in {
			forAll {
				instance : Company =>
					val differentStatus = instance.changeStatusTo[ErrorOr] (
						CompanyStatus.Inactive
						)
						.orFail ()

					val changedTimestamps = Company.timestamps
						.replace (epoch) (instance)

					assert (instance.differsFrom (instance) === false)
					assert (instance.differsFrom (differentStatus) === true)
					assert (instance.differsFrom (changedTimestamps) === false)
				}
			}

		"support monocle lenses" in {
			forAll {
				instance : Company =>
					val newName : Company.Name = "A New Name"
					val newDescription : Company.Description =
						"""This is a multi-line
							  description!"""

					val endo = Company.name
						.replace (newName)
						.andThen (
							Company.description
								.replace (newDescription)
							)

					whenever (
						(instance.name !== newName) &&
						(instance.description !== newDescription)
						) {
						val altered = endo (instance)

						assert (instance.name !== altered.name)
						assert (instance.description !== altered.description)
						}
				}
			}

		"support higher-kinded 'unless'" in {
			forAll {
				instance : Company =>
					val populated = instance.unless (_.name.value.isEmpty) (_.id)
					val empty = instance.unless[Unit] (_.name.value.nonEmpty) {
						_ => fail ("should never be evaluated")
						}

					assert (populated.isDefined)
					assert (empty.isEmpty)
					assert (populated.exists (_ === Company.id.get (instance)))
				}
			}

		"support higher-kinded 'when'" in {
			forAll {
				instance : Company =>
					whenever (instance.status.canBecome (CompanyStatus.Inactive)) {
						val canBeDeactivated = CompanyStatusCanBecome (
							CompanyStatus.Inactive
							)

						val populated = instance.when (canBeDeactivated) (
							_.version
							)

						val empty = instance.when[Int](_.name.value.isEmpty) {
							_ => fail ("should never be evaluated")
							}

						assert (populated.isDefined)
						assert (empty.isEmpty)
						assert (
							populated.exists (_ === Company.version.get (instance))
							)
						}
				}
			}

		"be able to 'touch' version and modification times" in {
			import Company.{
				id,
				timestamps,
				version
				}


			forAll {
				instance : Company =>
					val touched = instance.touch[ErrorOr] ()
						.orFail ()

					assert (instance !== touched)
					assert (instance.differsFrom (touched) === true)
					assert (id.get (instance) === id.get (touched))
					assert (
						timestamps.get (instance) !== timestamps.get (touched)
						)

					assert (version.get (instance) !== version.get (touched))
				}
			}

		"detect when one instance differs from another" in {
			forAll {
				(a : Company, b : Company) =>
					whenever (a.name !== b.name) {
						assert (!a.differsFrom (a))
						assert (a.differsFrom (b))
						}
				}
			}

		"be able to infer slug changed company events" in {
			val newSlug = Slug[ErrorOr] ("a-different-slug").orFail (
				"unable to define a valid Slug"
				)

			forAll {
				instance : Company =>
					whenever (instance.slug !== newSlug) {
						val slugChanged = Company.slug
							.replace (newSlug) (instance)

						val events = instance
							.infer[CompanyChangeEvents] (slugChanged)
							.toEvents ()

						assert (events.size === 1)
						}
				}
			}

		"only infer requested company events" in {
			val newSlug = Slug[ErrorOr] ("a-different-slug").orFail (
				"unable to define a valid Slug"
				)

			forAll {
				instance : Company =>
					whenever (instance.slug !== newSlug) {
						val slugChanged = Company.slug
							.replace (newSlug) (instance)

						val events = instance
							.infer[CNil] (slugChanged)
							.toEvents ()

						assert (events.isEmpty)
						}
				}
			}
		}
}

