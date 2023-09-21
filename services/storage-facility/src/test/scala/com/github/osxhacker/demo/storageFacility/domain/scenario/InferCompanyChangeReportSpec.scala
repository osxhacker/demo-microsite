package com.github.osxhacker.demo.storageFacility.domain.scenario

import scala.language.postfixOps

import cats.effect.IO
import eu.timepit.refined
import org.scalatest.diagrams.Diagrams

import com.github.osxhacker.demo.chassis.domain.Slug
import com.github.osxhacker.demo.chassis.monitoring.logging.MockLoggerFactory
import com.github.osxhacker.demo.storageFacility.domain.{
	Company,
	CompanyStatus,
	ScopedEnvironment
	}


/**
 * The '''InferCompanyChangeReportSpec''' type defines the unit-tests which
 * certify
 * [[com.github.osxhacker.demo.storageFacility.domain.scenario.InferCompanyChangeReport]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class InferCompanyChangeReportSpec ()
	extends ScenarioSpec ()
		with Diagrams
{
	/// Class Imports
	import InferCompanyChangeReport._
	import refined.auto._


	"The InferCompanyChangeReport algorithm" must {
		"define 'HavingCreated' as an 'Ior.Right" in {
			_ =>
				assert (HavingCreated (createArbitrary[Company] ()).isRight)
			}

		"define 'HavingDeleted' as an 'Ior.Left" in {
			_ =>
				assert (HavingDeleted (createArbitrary[Company] ()).isLeft)
			}

		"define 'HavingModified' as an 'Ior.Both" in {
			_ =>
				val company = createArbitrary[Company] ()

				assert (HavingModified (company, company).isBoth)
			}

		"emit no log events when no changes exist" in {
			original =>
				implicit val loggerFactory = MockLoggerFactory[IO] ()
				implicit val env = ScopedEnvironment[IO] (
					createGlobalEnvironment (),
					original.tenant,
					original.correlationId
					)

				val result = InferCompanyChangeReport (None)

				result map {
					_ =>
						assert (loggerFactory.iterator.isEmpty)
					}
			}

		"emit one log event when a company is created" in {
			original =>
				implicit val loggerFactory = MockLoggerFactory[IO] ()
				implicit val env = ScopedEnvironment[IO] (
					createGlobalEnvironment (),
					original.tenant,
					original.correlationId
					)

				val result = InferCompanyChangeReport (
					HavingCreated (createArbitrary[Company] ())
					)

				result map {
					_ =>
						assert (loggerFactory.infoOrAbove ().size === 1)
					}
			}

		"emit one log event when a company is deleted" in {
			original =>
				implicit val loggerFactory = MockLoggerFactory[IO] ()
				implicit val env = ScopedEnvironment[IO] (
					createGlobalEnvironment (),
					original.tenant,
					original.correlationId
					)

				val result = InferCompanyChangeReport (
					HavingDeleted (createArbitrary[Company] ())
					)

				result map {
					_ =>
						assert (loggerFactory.infoOrAbove ().size === 1)
					}
			}

		"emit two log events when all properties are different" in {
			original =>
				implicit val loggerFactory = MockLoggerFactory[IO] ()
				implicit val env = ScopedEnvironment[IO] (
					createGlobalEnvironment (),
					original.tenant,
					original.correlationId
					)

				val before = createArbitrary[Company] ()
				val differentSlug = new Slug ("acme-shoes")

				assert (before.slug !== differentSlug)
				assert (before.status !== CompanyStatus.Suspended)

				val after = Company.slug
					.replace (differentSlug)
					.andThen (
						Company.id
							.replace (before.id)
						)
					.andThen (
						Company.status
							.replace (CompanyStatus.Suspended)
						) (createArbitrary[Company] ())

				val result = InferCompanyChangeReport (
					HavingModified (before, after)
					)

				result map {
					_ =>
						assert (loggerFactory.infoOrAbove ().size === 2)
					}
			}
		}
}

