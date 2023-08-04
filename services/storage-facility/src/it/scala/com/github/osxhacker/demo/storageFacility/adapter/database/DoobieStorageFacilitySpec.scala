package com.github.osxhacker.demo.storageFacility.adapter.database

import cats.effect.IO
import eu.timepit.refined
import shapeless.{
	syntax => _,
	_
	}

import squants.space.CubicMeters

import com.github.osxhacker.demo.chassis.domain.Slug
import com.github.osxhacker.demo.chassis.domain.entity.{
	Identifier,
	ModificationTimes,
	Version
	}

import com.github.osxhacker.demo.chassis.domain.error.{
	LogicError,
	ObjectNotFoundError
	}

import com.github.osxhacker.demo.chassis.domain.repository._
import com.github.osxhacker.demo.storageFacility.domain._


/**
 * The '''DoobieStorageFacilitySpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.storageFacility.adapter.database.DoobieStorageFacility]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class DoobieStorageFacilitySpec ()
	extends IntegrationSpec (IntegrationSettings.DoobieStorageFacility)
{
	/// Class Imports
	import cats.syntax.traverse._
	import refined.api.RefType
	import refined.auto._


	/// Instance Properties
	private val owner = Company (
		id = Identifier.fromRandom[Company] (),
		slug = new Slug ("test-company"),
		name = "A Test Company",
		status = CompanyStatus.Active,
		timestamps = ModificationTimes.now ()
		)


	"The DoobieStorageFacility repository type" must {
		"be able to create a new StorageFacility" in withEnvironment {
			env =>
				val repo = env.storageFacilities
				val unsaved = createUnsaved ("Hello, world")
				val result = repo.save (CreateIntent (unsaved))

				result map {
					case Some (facility) =>
						assert (facility.id === unsaved.id)
						assert (facility.version === unsaved.version)
						assert (facility.status === unsaved.status)

					case None =>
						fail ("expected a storage facility")
					}
			}

		"disallow duplicate StorageFacility creations" in withEnvironment {
			env =>
				val repo = env.storageFacilities
				val unsaved = createUnsaved ("Try duplicate creations")
				val intent = CreateIntent (unsaved)
				val result = for {
					first <- repo.save (intent).attempt
					second <- repo.save (intent).attempt
					} yield (first, second)

				result map {
					case (first, second) =>
						assert (first.isRight)
						assert (second.isLeft)
					}
			}

		"be able to alter an existing StorageFacility" in withEnvironment {
			env =>
				val repo = env.storageFacilities
				val other = createUnsaved (
					"First one to make sure it is unaltered"
					)

				val unsaved = createUnsaved ("Original")
				val result = for {
					first <- repo.save (CreateIntent (other))
					created <- repo.save (CreateIntent (unsaved))
					updated <- repo.save (
						created.fold[Intent[StorageFacility]] (Ignore) (
							UpdateIntent (_)
							)
						)
					} yield (first, created, updated)

				result map {
					case (first, created, updated) =>
						assert (first.isDefined)
						assert (created.isDefined)
						assert (updated.isDefined)
						assert (first.exists (_.version === Version.initial))
						assert (first.map (_.id) !== created.map (_.id))
						assert (first.map (_.id) !== updated.map (_.id))
						assert (
							created.map (_.version) < updated.map (_.version)
							)
					}
			}

		"be able to find an existing StorageFacility" in withEnvironment {
			env =>
				val repo = env.storageFacilities
				val unsaved = createUnsaved ("Find instance")
				val result = for {
					saved <- repo.save (CreateIntent (unsaved))
						.map (_.getOrElse (fail ("unable to save instance")))

					found <- repo.find (saved.id)
					} yield (saved, found)

				result map {
					case (original, retrieved) =>
						assert (original.id === retrieved.id)
						assert (original.version === retrieved.version)
					}
			}

		"be able to support upsert StorageFacility instances" in withEnvironment {
			env =>
				val repo = env.storageFacilities
				val unsaved = createUnsaved ("An instance to upsert")
				val result = for {
					first <- repo.save (UpsertIntent (unsaved))
					second <- repo.save (
						UpsertIntent (first.orFail ("first save is missing"))
						)

					third <- first.orFail ("second save is missing")
						.touch[IO] ()
						.map (UpsertIntent (_))
						.flatMap (repo.save)

					all <- repo.findAll ()
						.compile
						.toList
					} yield (all, first :: second :: third :: Nil)

				result map {
					case (loaded, saved) =>
						assert (loaded.size === 1)
						assert (saved.size === 3)
						assert (saved.forall (_.isDefined))
					}
			}

		"be able to find multiple StorageFacility instances" in withEnvironment {
			env =>
				val repo = env.storageFacilities
				val expected = 5
				val fiveUnsaved = (1 to expected).map {
					n =>
						RefType.applyRef[StorageFacility.Name] (s"Instance #$n")
							.fold (
								fail (_),
								name => UpsertIntent (createUnsaved (name))
								)
						}
					.toList

				val result = for {
					saved <- fiveUnsaved.traverse (repo.save)
					all <- repo.findAll ()
						.compile
						.toList
					} yield (all, saved)

				result map {
					case (loaded, saved) =>
						assert (loaded.size === expected)
						assert (saved.size === loaded.size)
						assert (saved.forall (_.isDefined))
					}
			}

		"be able to detect StorageFacility duplicates" in withEnvironment {
			env =>
				val repo = env.storageFacilities
				val first = createUnsaved ("This will be a duplicate")
				val second = createUnsaved ("This will be a duplicate")
				val result = repo.save (CreateIntent (first)) >>
					repo.save (CreateIntent (second))
						.attempt

				result map {
					case Left (LogicError (message, Some (cause))) =>
						assert (message.nonEmpty)
						assert (cause ne null)

					case other =>
						fail ("expected a logic error, got: " + other)
					}
			}

		"be able to detect stale object updates/upserts" in withEnvironment {
			env =>
				val repo = env.storageFacilities
				val unsaved = createUnsaved ("An instance to upsert")
				val result = for {
					first <- repo.save (UpsertIntent (unsaved))
					second <- repo.save (
						UpdateIntent (
							first.orFail ("first save is missing")
							)
						)
						.attempt
				} yield first :: second :: HNil

				result map {
					case good :: duplicate :: HNil =>
						assert (good.isDefined)
						assert (duplicate.isRight)
					}
			}

		"gracefully detect when a StorageFacility does not exist" in withEnvironment {
			env =>
				val result = env.storageFacilities
					.exists (Identifier.fromRandom[StorageFacility] ())

				result map {
					instance =>
						assert (instance.isEmpty)
					}
			}

		"produce an 'ObjectNotFoundError' when finding missing instance" in withEnvironment {
			env =>
				val id = Identifier.fromRandom[StorageFacility] ()
				val result = env.storageFacilities
					.find (id)
					.attempt

				result map {
					case Left (ObjectNotFoundError (whatId, _)) =>
						assert (id === whatId)

					case other =>
						fail (s"expected error not detected, got: $other")
					}
			}
	}


	private def createUnsaved (name : StorageFacility.Name) : StorageFacility =
		StorageFacility (
			id = Identifier.fromRandom[StorageFacility] (),
			version = Version.initial,
			owner = owner,
			status = StorageFacilityStatus.UnderConstruction,
			name = name,
			city = "Anytown",
			state = "KS",
			zip = "90210",
			capacity = CubicMeters (1000),
			available = CubicMeters (1000),
			timestamps = ModificationTimes.now ()
			)


	/**
	 * No matter the result of a test, ensure there are no lingering
	 * integration test entity instances for the next test and that the schema
	 * is defined beforehand.  This is guaranteed by having sbt run integration
	 * tests sequentially.
	 */
	private def withEnvironment[A] (
		testCode : GlobalEnvironment[IO] => IO[A]
		)
		: IO[A] =
		withGlobalEnvironment {
			IO (_).flatTap (_.companies.save (UpsertIntent (owner)))
				.bracket (testCode) {
					env =>
						env.storageFacilities
							.findAll ()
							.evalMapChunk (env.storageFacilities.delete)
							.compile
							.drain >>
							env.companies
								.delete (owner)
								.void
					}
			}
}

