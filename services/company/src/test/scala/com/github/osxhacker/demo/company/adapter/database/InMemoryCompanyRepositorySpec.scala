package com.github.osxhacker.demo.company.adapter.database

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import eu.timepit.refined
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AsyncWordSpecLike
import org.typelevel.log4cats.noop.NoOpFactory
import shapeless.{
	syntax => _,
	_
	}

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.chassis.domain.entity.Identifier
import com.github.osxhacker.demo.chassis.domain.error.{
	ConflictingObjectsError,
	ObjectNotFoundError
	}

import com.github.osxhacker.demo.chassis.domain.repository._
import com.github.osxhacker.demo.company.domain.{
	Company,
	CompanySupport
	}

import com.github.osxhacker.demo.company.domain.repository.CompanyRepository


/**
 * The '''InMemoryCompanyRepositorySpec''' type defines the unit-tests which
 * certify
 * [[com.github.osxhacker.demo.company.adapter.database.InMemoryCompanyRepository]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class InMemoryCompanyRepositorySpec ()
	extends AsyncIOSpec
		with AsyncWordSpecLike
		with Diagrams
		with ProjectSpec
		with CompanySupport
{
	/// Class Imports
	import cats.syntax.traverse._
	import refined.auto._


	/// Instance Properties
	implicit private val noopFactory = NoOpFactory[IO]


	"The InMemoryCompanyRepository repository type" must {
		"be able to create a new Company" in withRepository {
			repo =>
				val unsaved = createArbitrary[Company] ()
				val result = repo.save (CreateIntent (unsaved))

				result map {
					case Some (company) =>
						assert (company.id === unsaved.id)
						assert (company.version >= unsaved.version)
						assert (company.status === unsaved.status)

					case None =>
						fail ("expected a company")
					}
			}

		"disallow duplicate Company creations" in withRepository {
			repo =>
				val unsaved = createArbitrary[Company] ()
				val intent = CreateIntent (unsaved)
				val result = for {
					first <- repo.save (intent)
						.attempt

					second <- repo.save (intent)
						.attempt
					} yield (first, second)

				result map {
					case (first, second) =>
						assert (first.isRight)
						assert (second.isLeft)
					}
			}

		"be able to alter an existing Company" in withRepository {
			repo =>
				val other = createArbitrary[Company] ()
				val unsaved = createArbitrary[Company] ()

				assert (other !== unsaved)

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
						assert (created.map (_.version) < updated.map (_.version))
					}
			}

		"be able to find an existing Company" in withRepository {
			repo =>
				val unsaved = createArbitrary[Company] ()
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

		"be able to support upsert Company instances" in withRepository {
			repo =>
				val unsaved = createArbitrary[Company] ()
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

		"be able to find multiple Company instances" in withRepository {
			repo =>
				val expected = 5
				val fiveUnsaved = (1 to expected)
					.map {
						_ =>
							UpsertIntent (createArbitrary[Company] ())
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

		"be able to detect Company duplicates" in withRepository {
			repo =>
				val first = createArbitrary[Company] ()
				val second = Company.name
					.replace ("A Different Name")
					.andThen (
						Company.id
							.replace (Identifier.fromRandom[Company] ())
						) (first)

				val result = repo.save (CreateIntent (first)) >>
					repo.save (CreateIntent (second))
						.attempt

				result map {
					case Left (ConflictingObjectsError (message, _)) =>
						assert (message.nonEmpty)

					case other =>
						fail (
							"expected a conflicting objects error, got: " + other
							)
					}
			}

		"be able to detect stale object updates/upserts" in withRepository {
			repo =>
				val unsaved = createArbitrary[Company] ()
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


	/**
	 * No matter the result of a test, ensure there are no lingering
	 * [[com.github.osxhacker.demo.company.domain.Company]] instances for the
	 * next test and that the schema is defined beforehand.  This is guaranteed
	 * by having sbt run integration tests sequentially.
	 */
	private def withRepository[A] (
		testCode : CompanyRepository[IO] => IO[A]
		)
		: IO[A] =
		testCode (InMemoryCompanyRepository[IO] ())
}

