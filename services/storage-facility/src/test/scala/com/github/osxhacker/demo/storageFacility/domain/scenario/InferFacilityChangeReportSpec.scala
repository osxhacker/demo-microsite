package com.github.osxhacker.demo.storageFacility.domain.scenario

import scala.language.postfixOps

import cats.effect.IO
import eu.timepit.refined
import org.scalatest.diagrams.Diagrams

import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.chassis.monitoring.logging.MockLoggerFactory
import com.github.osxhacker.demo.storageFacility.domain.{
	ScopedEnvironment,
	StorageFacility,
	StorageFacilityStatus
	}


/**
 * The '''InferFacilityChangeReportSpec''' type defines the unit-tests which
 * certify
 * [[com.github.osxhacker.demo.storageFacility.domain.scenario.InferFacilityChangeReport]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class InferFacilityChangeReportSpec ()
	extends ScenarioSpec ()
		with Diagrams
{
	/// Class Imports
	import InferFacilityChangeReport._
	import refined.auto._


	"The InferFacilityChangeReport algorithm" must {
		"define 'HavingCreated' as an 'Ior.Right" in {
			_ =>
				assert (
					HavingCreated (createArbitrary[StorageFacility] ()).isRight
					)
			}

		"define 'HavingDeleted' as an 'Ior.Left" in {
			_ =>
				assert (
					HavingDeleted (createArbitrary[StorageFacility] ()).isLeft
					)
			}

		"define 'HavingModified' as an 'Ior.Both" in {
			_ =>
				val facility = createArbitrary[StorageFacility] ()

				assert (HavingModified (facility, facility).isBoth)
			}

		"emit no log events when no changes exist" in {
			original =>
				implicit val loggerFactory = MockLoggerFactory[IO] ()
				implicit val env = ScopedEnvironment[IO] (
					createGlobalEnvironment (),
					original.tenant,
					original.correlationId
					)

				val result = InferFacilityChangeReport (None)

				result map {
					_ =>
						assert (loggerFactory.iterator.isEmpty)
					}
			}

		"emit one log event when a facility is created" in {
			original =>
				implicit val loggerFactory = MockLoggerFactory[IO] ()
				implicit val env = ScopedEnvironment[IO] (
					createGlobalEnvironment (),
					original.tenant,
					original.correlationId
					)

				val result = InferFacilityChangeReport (
					HavingCreated (createArbitrary[StorageFacility] ())
					)

				result map {
					_ =>
						assert (loggerFactory.infoOrAbove ().size === 1)
					}
			}

		"emit one log event when a facility is deleted" in {
			original =>
				implicit val loggerFactory = MockLoggerFactory[IO] ()
				implicit val env = ScopedEnvironment[IO] (
					createGlobalEnvironment (),
					original.tenant,
					original.correlationId
					)

				val result = InferFacilityChangeReport (
					HavingDeleted (createArbitrary[StorageFacility] ())
					)

				result map {
					_ =>
						assert (loggerFactory.infoOrAbove ().size === 1)
					}
			}

		"emit one log event when all properties are different" in {
			original =>
				implicit val loggerFactory = MockLoggerFactory[IO] ()
				implicit val env = ScopedEnvironment[IO] (
					createGlobalEnvironment (),
					original.tenant,
					original.correlationId
					)

				val before = createArbitrary[StorageFacility] ()

				assert (before.status !== StorageFacilityStatus.Closed)

				val after = StorageFacility.status
					.replace (StorageFacilityStatus.Closed)
					.andThen (
						StorageFacility.id
							.replace (before.id)
						)
					.andThen (
						StorageFacility.version
							.replace (before.version.next[ErrorOr] ().orThrow ())
						) (createArbitrary[StorageFacility] ())

				val result = InferFacilityChangeReport (
					HavingModified (before, after)
					)

				result map {
					_ =>
						assert (loggerFactory.infoOrAbove ().size === 1)
					}
			}
		}
}

