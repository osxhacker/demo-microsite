package com.github.osxhacker.demo.storageFacility.adapter.database

import java.sql.SQLException

import cats.data.{
	IndexedStateT,
	Kleisli
	}

import cats.effect.MonadCancelThrow
import doobie._
import fs2.Stream
import org.typelevel.log4cats.LoggerFactory
import shapeless.{
	syntax => _,
	_
	}

import com.github.osxhacker.demo.chassis.domain.entity.{
	Identifier,
	ModificationTimes,
	Version
	}

import com.github.osxhacker.demo.chassis.domain.ChimneyErrors
import com.github.osxhacker.demo.chassis.domain.error._
import com.github.osxhacker.demo.chassis.domain.repository._
import com.github.osxhacker.demo.storageFacility
import com.github.osxhacker.demo.storageFacility.domain.{
	Company,
	StorageFacility
	}

import com.github.osxhacker.demo.storageFacility.domain.repository.StorageFacilityRepository

import schema.{
	CompanyRecord,
	StorageFacilityRecord,
	StorageFacilityStatusRecord
	}


/**
 * The '''DoobieStorageFacility''' type fulfills the
 * [[com.github.osxhacker.demo.storageFacility.domain.repository.StorageFacilityRepository]]
 * contract for managing the persistent store representation of
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]]
 * instances using [[doobie]].
 */
final class DoobieStorageFacility[F[_]] (
	private val transactor : Transactor[F]
	)
	(
		implicit

		/// Needed for `flatMap` and `transact`.
		private val monadCancelThrow : MonadCancelThrow[F],

		/// Needed for logging.
		private val loggerFactory : LoggerFactory[F]
	)
	extends StorageFacilityRepository[F]
{
	/// Class Imports
	import DoobieStorageFacility._
	import cats.syntax.all._
	import doobie.implicits._
	import io.scalaland.chimney.dsl._
	import mouse.option._
	import storageFacility.domain.transformers._


	/// Class Types
	private object detectErrors
		extends PostgresErrorHandling[StorageFacility] ()
	{
		/// Class Imports
		import StorageFacility.{
			id,
			version
			}


		/// Instance Properties
		lazy val whenDeleting = stale (id, version)
			.orHandleWith[Int] (defaultHandler ("delete"))
			.ensure (_ <= 1) {
				case (facility, other) =>
					LogicError (
						new StringBuilder ()
							.append ("deleted more than one storage facility: ")
							.append (other)
							.append (' ')
							.append (id.get (facility).show)
							.append (' ')
							.append (version.get (facility).show)
							.toString ()
						)
				}

		lazy val whenSaving =
			(
				stale (id, version) |+|
				duplicate () |+|
				constraintViolation (id, version)
			)
				.orHandleWith[Int] (defaultHandler ("save"))
				.ensure (_ === 1) {
					case (facility, 0) =>
						StaleObjectError[StorageFacility] (
							id.get (facility),
							version.get (facility)
							)

					case (facility, other) =>
						LogicError (
							new StringBuilder ()
								.append ("updated more than one storage facility: ")
								.append (other)
								.append (' ')
								.append (id.get (facility).show)
								.append (' ')
								.append (version.get (facility).show)
								.toString ()
							)
					}


		private def defaultHandler (opName : String)
			(instance : StorageFacility, ex : SQLException)
			: UnknownPersistenceError =
			UnknownPersistenceError (
				new StringBuilder ()
					.append ("unable to ")
					.append (opName)
					.append (" storage facility: ")
					.append (id.get (instance).show)
					.append (' ')
					.append (version.get (instance).show)
					.toString (),

				ex.some
				)
	}


	/// Instance Properties
	private val mkRecord =
		Kleisli[
			F,
			StorageFacility :: PrimaryKey[CompanyRecord] :: HNil,
			StorageFacilityRecord
			] {
			_.transformIntoF[ChimneyErrors, StorageFacilityRecord]
				.toEither
				.leftMap {
					errors =>
						new RuntimeException (errors.mkString_ (","))
					}
				.liftTo[F]
			}


	private val mkStorageFacility =
		Kleisli[F, QueryResultsType, StorageFacility] {
			_.transformIntoF
				.toEither
				.leftMap {
					errors =>
						new RuntimeException (errors.mkString_ (","))
					}
				.liftTo[F]
			}

	private val saveSteps = for {
		facility <- IndexedStateT.inspect[F, ExecutableIntent[StorageFacility], StorageFacility] {
			_.value
			}

		companyKey <- IndexedStateT.liftF[F, ExecutableIntent[StorageFacility], PrimaryKey[CompanyRecord]] {
			CompanyQueries.FindKeyById (facility.owner.id.toUuid ())
				.transact (transactor)
			}

		_ <- IndexedStateT.modifyF[F, ExecutableIntent[StorageFacility], Update0] {
			case CreateIntent (instance) =>
				mkRecord.map (StorageFacilityMutations.Insert (_)).run (instance :: companyKey :: HNil)

			case UpdateIntent (instance) =>
				mkRecord.map (StorageFacilityMutations.Update (_)).run (instance :: companyKey :: HNil)

			case UpsertIntent (instance) =>
				mkRecord.map (StorageFacilityMutations.Upsert (_)).run (instance :: companyKey :: HNil)
			}

		saved <- IndexedStateT.inspectF[F, Update0, QueryResultsType] {
			_.run
				.attemptSql
				.flatMap (detectErrors.whenSaving[ConnectionIO] (facility))
				.as (facility.id.toUuid ())
				.flatMap (StorageFacilityQueries.Refresh (_))
				.transact (transactor)
			}

		latest <- IndexedStateT.liftF (mkStorageFacility (saved))
		} yield latest.some


	override def createSchema () : F[Int] =
		(
			CreateAndSeedTableFor[StorageFacilityStatusRecord] () |+|
			CreateAndSeedTableFor[StorageFacilityRecord] ()
		)
			.liftTo[F]
			.flatMap {
				_.foldLeftM (0) {
					(count, sql) =>
						sql.run
							.transact (transactor)
							.map (count + _)
					}
				}


	override def delete (instance : StorageFacility) : F[Boolean] =
		CompanyQueries.FindKeyById (instance.owner.id.toUuid ())
			.flatMap {
				companyKey =>
					(instance :: companyKey :: HNil)
						.transformIntoF[ChimneyErrors, StorageFacilityRecord]
						.bimap (
							errors => new RuntimeException (errors.mkString_ (",")),
							StorageFacilityMutations.Delete (_)
							)
						.fold (
							MonadCancelThrow[ConnectionIO].raiseError[Int],
							_.run
							)
				}
			.attemptSql
			.flatMap (detectErrors.whenDeleting[ConnectionIO] (instance))
			.transact (transactor)
			.map (_ === 1)


	override def exists (id : Identifier[StorageFacility])
		: F[Option[Version]] =
		StorageFacilityQueries.Exists (id.toUuid ())
			.transact (transactor)


	override def find (id : Identifier[StorageFacility]) : F[StorageFacility] =
		StorageFacilityQueries.FindById (id.toUuid ())
			.transact (transactor)
			.flatMap {
				_.cata (
					mkStorageFacility (_),
					monadCancelThrow.raiseError (
						ObjectNotFoundError (id)
						)
					)
				}


	override def findAll () : Stream[F, StorageFacility] =
		StorageFacilityQueries.FindAll ()
			.transact (transactor)
			.evalMapChunk (mkStorageFacility.run)


	override def save (intent : Intent[StorageFacility])
		: F[Option[StorageFacility]] =
		intent match {
			case ei : ExecutableIntent[StorageFacility] =>
				saveSteps.runA (ei)

			case Ignore =>
				loggerFactory.create
					.flatMap (_.debug ("ignoring storage facility save"))
					.as (none[StorageFacility])
			}
}


object DoobieStorageFacility
	extends CompanyDatabaseImplicits
{
	/// Class Imports
	import cats.syntax.either._
	import io.scalaland.chimney.TransformerF
	import io.scalaland.chimney.cats._
	import io.scalaland.chimney.dsl._
	import shapeless.nat._1
	import storageFacility.domain.transformers._


	/// Class Types
	type QueryResultsType = StorageFacilityQueries.QueryResultsType


	/// Implicit Conversions
	implicit private val fromStorageFacilityRecords
		: TransformerF[
			ChimneyErrors,
			StorageFacilityQueries.QueryResultsType,
			StorageFacility
			] =
		new TransformerF[
			ChimneyErrors,
			StorageFacilityQueries.QueryResultsType,
			StorageFacility
		] {
			override def transform (src : StorageFacilityQueries.QueryResultsType)
				: ChimneyErrors[StorageFacility] =
				src.at[_0]
					.intoF[ChimneyErrors, StorageFacility]
					.enableOptionDefaultsToNone
					.withFieldRenamed (_.external_id, _.id)
					.withFieldRenamed (_.region, _.primary)
					.withFieldConstF (
						_.owner,
						src.at[_1]
							.intoF[ChimneyErrors, Company]
							.transform
						)
					.withFieldComputed (
						_.timestamps,
						rec => ModificationTimes (
							rec.created_on,
							rec.last_changed
							)
						)
					.transform
		}

	implicit val toStorageFacilityRecord
		: TransformerF[
			ChimneyErrors,
			StorageFacility :: PrimaryKey[CompanyRecord] :: HNil,
			StorageFacilityRecord
			] =
		new TransformerF[
			ChimneyErrors,
			StorageFacility :: PrimaryKey[CompanyRecord] :: HNil,
			StorageFacilityRecord
			] {
			override def transform (
				src : StorageFacility :: PrimaryKey[CompanyRecord] :: HNil
				)
				: ChimneyErrors[StorageFacilityRecord] =
				src.at[_0]
					.intoF[ChimneyErrors, StorageFacilityRecord]
					.enableOptionDefaultsToNone
					.withFieldRenamed (_.id, _.external_id)
					.withFieldRenamed (_.primary, _.region)
					.withFieldConstF (
						_.company_key,
						StorageFacilityRecord.Company_keyType
							.from (src.at[_1].key)
							.leftMap (_.getMessage)
							.toValidatedNec
						)
					.withFieldComputed (_.created_on, _.timestamps.createdOn)
					.withFieldComputed (_.last_changed, _.timestamps.lastChanged)
					.transform
		}
}

