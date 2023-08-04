package com.github.osxhacker.demo.company.adapter.kafka

import java.util.UUID.randomUUID

import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.postfixOps

import cats.data.Kleisli
import cats.effect.IO
import cats.effect.kernel.Deferred
import shapeless.{
	syntax => _,
	_
	}

import shapeless.ops.nat
import com.github.osxhacker.demo.chassis
import com.github.osxhacker.demo.chassis.domain.{
	Slug,
	Specification
	}

import com.github.osxhacker.demo.chassis.effect.ReadersWriterResource
import com.github.osxhacker.demo.company.adapter.rest.api
import com.github.osxhacker.demo.company.domain._
import com.github.osxhacker.demo.company.domain.event._


/**
 * The '''ConsumeCompanyEventsSpec''' type defines the integration-tests which
 * certify
 * [[com.github.osxhacker.demo.company.adapter.kafka.PublishCompanyEvents]] for
 * fitness of purpose and serves as an exemplar of its use.
 */
final class ConsumeCompanyEventsSpec ()
	extends IntegrationSpec (IntegrationSettings.ConsumeCompanyEvents)
{
	/// Class Imports
	import ConsumeCompanyEventsSpec._
	import cats.syntax.applicativeError._
	import cats.syntax.show._
	import chassis.syntax._
	import shapeless.nat._


	/// Instance Properties
	private val maximumDeliveryDuration = 30 seconds


	"The ConsumeCompanyEvents type" must {
		"be able to read a api.CompanyCreated event" in withGlobalEnvironment {
			case (settings, global) =>
				for {
					guarded <- ReadersWriterResource.from[IO, GlobalEnvironment[IO]] (
						global
						)

					implicit0 (scoped : ScopedEnvironment[IO]) <- global.scopeWith (
						randomUUID ().toString
						)

					eventFound <- Deferred[IO, CompanyEventType]

					consumer = ConsumeCompanyEvents[IO] (
						global.companyEvents.channel,
						global.fingerprint,
						guarded,
						settings
						)

					event = CompanyCreated (createCompany ())
					scan = ScanFor (_.id.value === event.id.show, eventFound)

					_ <- IO.unit
						.addEvent (event)
						.broadcast ()

					/// Start the consumer as a Fiber so that either the test
					/// finds the event it is looking for or it times-out.
					active <- consumer (scan ().handleError (_ => {}))
						.start

					result <- IO.race (
						IO.sleep (maximumDeliveryDuration),
						eventFound.get
						)

					_ <- active.cancel
					} yield {
						assert (result.isRight)
						assert (result.exists (_.id.value === event.id.show))
						assert (
							result.exists (_.origin.region.value === event.region.show)
							)
						}
			}

		"be able to read multiple api.CompanyEvent events" in withGlobalEnvironment {
			case (settings, global) =>
				val company = createCompany ()

				for {
					guarded <- ReadersWriterResource.from[IO, GlobalEnvironment[IO]] (
						global
						)

					implicit0 (scoped : ScopedEnvironment[IO]) <- global.scopeWith (
						randomUUID ().toString
						)

					oldSlug <- Slug[IO] ("a-different-test-slug")
					eventsFound <- Deferred[IO, Seq[CompanyEventType]]

					consumer = ConsumeCompanyEvents[IO] (
						global.companyEvents.channel,
						global.fingerprint,
						guarded,
						settings
						)

					created = CompanyCreated (company)
					slugChanged = CompanySlugChanged (company, oldSlug)

					scan = ScanForAtLeast[_2] (
						_.id.value === company.id.show,
						eventsFound
						)

					_ <- IO.unit
						.addEvents (created :: slugChanged :: HNil)
						.broadcast ()

					/// Start the consumer as a Fiber so that either the test
					/// finds the event it is looking for or it times-out.
					active <- consumer (scan ().handleError (_ => {})).start

					result <- IO.race (
						IO.sleep (maximumDeliveryDuration),
						eventsFound.get
						)

					_ <- active.cancel
				} yield {
					assert (result.isRight)
					assert (result.exists (_.size === 2))

					result foreach {
						case Seq (created, changed) =>
							assert (created.isInstanceOf[api.CompanyCreated])
							assert (changed.isInstanceOf[api.CompanySlugChanged])

							assert (created.id === changed.id)
							assert (created.origin.region === changed.origin.region)

						case other =>
							fail (s"unexpected events: $other")
						}
					}
			}
		}
}


object ConsumeCompanyEventsSpec
{
	/// Class Types
	final case class ScanFor (
		private val specification : Specification[CompanyEventType],
		private val instance : Deferred[IO, CompanyEventType]
		)
	{
		/// Class Imports
		import mouse.boolean._


		def apply ()
			: Kleisli[IO, (CompanyEventType, GlobalEnvironment[IO]), Unit] =
			Kleisli {
				case (event, _) =>
					specification (event).whenAL {
						instance.complete (event)
						}
				}
	}


	final case class ScanForAtLeast[HowMany <: Nat] (
		private val specification : Specification[CompanyEventType],
		private val instance : Deferred[IO, Seq[CompanyEventType]]
		)
		(implicit private val howMany : nat.ToInt[HowMany])
	{
		/// Class Imports
		import cats.syntax.eq._
		import mouse.boolean._


		/// Instance Properties
		private val matches = new mutable.ListBuffer[CompanyEventType]


		def apply ()
			: Kleisli[IO, (CompanyEventType, GlobalEnvironment[IO]), Unit] =
			Kleisli {
				case (event, _) if specification (event) =>
					(matches.addOne (event).size === howMany ()).whenAL {
						instance.complete (matches.toSeq)
						}
				}
	}
}
