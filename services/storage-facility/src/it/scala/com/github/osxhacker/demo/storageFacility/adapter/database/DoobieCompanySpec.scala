package com.github.osxhacker.demo.storageFacility.adapter.database

import cats.effect.IO
import eu.timepit.refined
import shapeless.{
	syntax => _,
	_
	}

import com.github.osxhacker.demo.chassis.domain.Slug
import com.github.osxhacker.demo.chassis.domain.entity.{
	Identifier,
	ModificationTimes
	}

import com.github.osxhacker.demo.chassis.domain.error.{
	LogicError,
	ObjectNotFoundError
	}

import com.github.osxhacker.demo.chassis.domain.repository._
import com.github.osxhacker.demo.storageFacility.domain.{
	Company,
	CompanyStatus
	}

import com.github.osxhacker.demo.storageFacility.domain.repository.CompanyRepository


/**
 * The '''DoobieCompanySpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.storageFacility.adapter.database.DoobieCompany]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class DoobieCompanySpec ()
	extends IntegrationSpec (IntegrationSettings.DoobieCompany)
{
	/// Class Imports
	import cats.syntax.traverse._
	import refined.api.RefType
	import refined.auto._


	"The DoobieCompany repository type" must {
		"be able to create a new Company" in withRepository {
			repo =>
				val unsaved = createUnsaved ("test", "Hello, world")
				val result = repo.save (CreateIntent (unsaved))

				result map {
					case Some (facility) =>
						assert (facility.id === unsaved.id)
						assert (facility.status === unsaved.status)

					case None =>
						fail ("expected a company")
					}
			}

		"disallow duplicate Company creations" in withRepository {
			repo =>
				val unsaved = createUnsaved ("test", "Try duplicate creations")
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

		"be able to alter an existing Company" in withRepository {
			repo =>
				val other = createUnsaved (
					"first",
					"First one to make sure it is unaltered"
					)

				val unsaved = createUnsaved ("original", "Original")
				val result = for {
					first <- repo.save (CreateIntent (other))
					created <- repo.save (CreateIntent (unsaved))
					updated <- repo.save (
						created.fold[Intent[Company]] (Ignore) (
							UpdateIntent (_)
							)
						)
					} yield (first, created, updated)

				result map {
					case (first, created, updated) =>
						assert (first.isDefined)
						assert (created.isDefined)
						assert (updated.isDefined)
						assert (first.map (_.id) !== created.map (_.id))
						assert (first.map (_.id) !== updated.map (_.id))
					}
			}

		"be able to find an existing Company" in withRepository {
			repo =>
				val unsaved = createUnsaved ("test", "Find instance")
				val result = for {
					saved <- repo.save (CreateIntent (unsaved))
						.map (_.getOrElse (fail ("unable to save instance")))

					found <- repo.find (saved.id)
					} yield (saved, found)

				result map {
					case (original, retrieved) =>
						assert (original.id === retrieved.id)
					}
			}

		"be able to support upsert Company instances" in withRepository {
			repo =>
				val unsaved = createUnsaved ("test", "An instance to upsert")
				val result = for {
					first <- repo.save (UpsertIntent (unsaved))
					second <- repo.save (
						UpsertIntent (first.orFail ("first save is missing"))
						)

					third <- repo.save (
						UpsertIntent (second.orFail ("second save is missing"))
						)

					all <- repo.findAll ()
						.compile
						.toList
					} yield (all, first :: second :: third :: Nil)

				result map {
					case (loaded, saved) =>
						assert (loaded.size === 2)
						assert (saved.size === 3)
						assert (saved.forall (_.isDefined))
					}
			}

		"be able to find multiple Company instances" in withRepository {
			repo =>
				val howManyToCreate =  5
				val expected = howManyToCreate + 1
				val fiveUnsaved = (1 to howManyToCreate).map {
					n =>
						RefType.applyRef[Company.Name] (s"Instance #$n")
							.flatMap {
								name =>
									RefType.applyRef[Slug.Value] (s"test-$n")
										.map (_ -> name)
								}
							.fold (
								fail (_),
								pair =>
									UpsertIntent (
										(createUnsaved _).tupled (pair)
										)
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
						assert (saved.size === howManyToCreate)
						assert (saved.forall (_.isDefined))
					}
			}

		"be able to detect Company duplicates" in withRepository {
			repo =>
				val first = createUnsaved ("dup", "This will be a duplicate")
				val second = createUnsaved ("dup", "This will be a duplicate")
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

		"be able to detect stale object updates/upserts" in withRepository {
			repo =>
				val unsaved = createUnsaved ("stale", "An instance to upsert")
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

		"gracefully detect when a Company does not exist" in withRepository {
			repo =>
				val result = repo.exists (Identifier.fromRandom[Company] ())

				result map {
					instance =>
						assert (instance.isEmpty)
					}
			}

		"produce an 'ObjectNotFoundError' when finding missing instance" in withRepository {
			repo =>
				val id = Identifier.fromRandom[Company] ()
				val result = repo.find (id)
					.attempt

				result map {
					case Left (ObjectNotFoundError (whatId, _)) =>
						assert (id === whatId)

					case other =>
						fail (s"expected error not detected, got: $other")
					}
			}
	}


	private def createUnsaved (
		slug : Slug.Value,
		name : Company.Name
		) : Company =
		Company (
			id = Identifier.fromRandom[Company] (),
			slug = Slug (slug),
			status = CompanyStatus.Active,
			name = name,
			timestamps = ModificationTimes.now ()
			)


	/**
	 * No matter the result of a test, ensure there are no lingering
	 * [[com.github.osxhacker.demo.storageFacility.domain.Company]]
	 * instances for the next test and that the schema is defined beforehand.
	 * This is guaranteed by having sbt run integration tests sequentially.
	 */
	private def withRepository[A] (
		testCode : CompanyRepository[IO] => IO[A]
		)
		: IO[A] =
		withGlobalEnvironment {
			global =>
			IO (global.companies).bracket (testCode) {
				repo =>
					repo.findAll ()
						.evalMapChunk (repo.delete)
						.compile
						.drain
				}
			}
}

