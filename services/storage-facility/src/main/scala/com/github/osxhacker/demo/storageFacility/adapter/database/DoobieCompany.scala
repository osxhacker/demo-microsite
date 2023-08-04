package com.github.osxhacker.demo.storageFacility.adapter.database

import java.sql.SQLException

import cats.data.{
	IndexedStateT,
	Kleisli,
	ValidatedNec
	}

import cats.effect.MonadCancelThrow
import doobie._
import fs2.Stream
import org.typelevel.log4cats.LoggerFactory

import com.github.osxhacker.demo.chassis.domain.error._
import com.github.osxhacker.demo.chassis.domain.entity.{
	Identifier,
	ModificationTimes,
	Version
	}

import com.github.osxhacker.demo.chassis.domain.repository._
import com.github.osxhacker.demo.storageFacility
import com.github.osxhacker.demo.storageFacility.domain.{
	Company,
	CompanyReference
	}

import com.github.osxhacker.demo.storageFacility.domain.repository.CompanyRepository

import schema.{
	CompanyRecord,
	CompanyStatusRecord
	}


/**
 * The '''DoobieCompany''' type fulfills the
 * [[com.github.osxhacker.demo.storageFacility.domain.repository.CompanyRepository]]
 * contract for managing the persistent store representation of
 * [[com.github.osxhacker.demo.storageFacility.domain.Company]] instances using
 * [[doobie]].
 */
final class DoobieCompany[F[_]] (
	private val transactor : Transactor[F]
	)
	(
		implicit

		/// Needed for `flatMap` and `transact`.
		private val monadCancelThrow : MonadCancelThrow[F],

		/// Needed for logging.
		private val loggerFactory : LoggerFactory[F]
	)
	extends CompanyRepository[F]
{
	/// Class Imports
	import DoobieCompany._
	import cats.syntax.all._
	import doobie.implicits._
	import io.scalaland.chimney.dsl._
	import mouse.option._


	/// Class Types
	private object detectErrors
		extends PostgresErrorHandling[Company] ()
	{
		/// Class Imports
		import Company.id


		/// Instance Properties
		lazy val whenDeleting = notFound[Company] (id)
			.orHandleWith[Int] (defaultHandler ("delete"))
			.ensure (_ <= 1) {
				case (company, other) =>
					LogicError (
						new StringBuilder ()
							.append ("deleted more than one company: ")
							.append (other)
							.append (' ')
							.append (id.get (company).show)
							.toString ()
					)
				}

		lazy val whenSaving =
			(
				notFound[Company] (id) |+|
				duplicate () |+|
				constraintViolation (id)
			)
				.orHandleWith[Int] (defaultHandler ("save"))
				.ensure (_ === 1) {
					case (company, 0) =>
						ObjectNotFoundError[Company] (id.get (company))

					case (company, other) =>
						LogicError (
							new StringBuilder ()
								.append ("updated more than one company: ")
								.append (other)
								.append (' ')
								.append (id.get (company).show)
								.toString ()
							)
					}


		private def defaultHandler (opName : String)
			(instance : Company, ex : SQLException)
			: UnknownPersistenceError =
			UnknownPersistenceError (
				new StringBuilder ()
					.append ("unable to ")
					.append (opName)
					.append (" company: ")
					.append (id.get (instance).show)
					.toString (),

				ex.some
				)
	}


	/// Instance Properties
	private val mkRecord = Kleisli[F, Company, CompanyRecord] {
		_.transformIntoF
			.toEither
			.leftMap {
				errors =>
					DomainValueError (errors.mkString_ (","))
				}
			.liftTo[F]
		}

	private val mkCompany =  Kleisli[F, CompanyRecord, Company] {
		_.transformIntoF
			.toEither
			.leftMap {
				errors =>
					DomainValueError (errors.mkString_ (","))
				}
			.liftTo[F]
		}

	private val saveSteps = for {
		company <- IndexedStateT.inspect[F, ExecutableIntent[Company], Company] {
			_.value
			}

		_ <- IndexedStateT.modifyF[F, ExecutableIntent[Company], Update0] {
			case CreateIntent (instance) =>
				mkRecord.map (CompanyMutations.Insert (_))
					.run (instance)

			case UpdateIntent (instance) =>
				mkRecord.map (CompanyMutations.Update (_))
					.run (instance)

			case UpsertIntent (instance) =>
				mkRecord.map (CompanyMutations.Upsert (_))
					.run (instance)
		}

		saved <- IndexedStateT.inspectF[F, Update0, CompanyRecord] {
			_.run
				.attemptSql
				.flatMap (detectErrors.whenSaving[ConnectionIO] (company))
				.as (company.id.toUuid ())
				.flatMap (CompanyQueries.Refresh (_))
				.transact (transactor)
		}

		latest <- IndexedStateT.liftF (mkCompany (saved))
	} yield latest.some


	override def createSchema () : F[Int] =
		(
			CreateAndSeedTableFor[CompanyStatusRecord] () |+|
			CreateAndSeedTableFor[CompanyRecord] ()
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


	override def delete (instance : Company) : F[Boolean] =
		mkRecord.flatMapF {
			CompanyMutations.Delete (_)
				.run
				.attemptSql
				.flatMap (detectErrors.whenDeleting[ConnectionIO] (instance))
				.transact (transactor)
				.map (_ === 1)
			}
			.run (instance)


	override def exists (id : Identifier[Company]) : F[Option[Version]] =
		CompanyQueries.Exists (id.toUuid ())
			.transact (transactor)


	override def find (reference : CompanyReference) : F[Company] =
		reference.fold (
			CompanyQueries.FindBySlug (_),
			id => CompanyQueries.FindById (id.toUuid ())
			)
			.transact (transactor)
			.flatMap {
				_.cata (
					mkCompany (_),
					monadCancelThrow.raiseError (
						reference.fold (
							slug => DomainValueError (
								s"unknown slug: ${slug.show}"
								),

							ObjectNotFoundError (_)
							)
						)
					)
				}


	override def find (id : Identifier[Company]) : F[Company] =
		CompanyQueries.FindById (id.toUuid ())
			.transact (transactor)
			.flatMap {
				_.cata (
					mkCompany (_),
					monadCancelThrow.raiseError (
						ObjectNotFoundError (id)
						)
					)
				}


	override def findAll () : Stream[F, Company] =
		CompanyQueries.FindAll ()
			.transact (transactor)
			.evalMapChunk (mkCompany (_))


	override def save (intent : Intent[Company]) : F[Option[Company]] =
		intent match {
			case ei : ExecutableIntent[Company] =>
				saveSteps.runA (ei)

			case Ignore =>
				loggerFactory.create
					.flatMap (_.debug ("ignoring company save"))
					.as (none[Company])
			}
}


object DoobieCompany
	extends CompanyDatabaseImplicits


private[database] trait CompanyDatabaseImplicits
{
	/// Class Imports
	import io.scalaland.chimney.TransformerF
	import io.scalaland.chimney.cats._
	import storageFacility.domain.transformers._


	/// Implicit Conversions
	implicit val fromCompanyRecord
		: TransformerF[
			ValidatedNec[String, +*],
			CompanyRecord,
			Company
			] =
		TransformerF.define[
			ValidatedNec[String, +*],
			CompanyRecord,
			Company
			]
			.withFieldRenamed (_.external_id, _.id)
			.withFieldComputed (
				_.timestamps,
				rec => ModificationTimes (rec.created_on, rec.last_changed)
				)
			.buildTransformer

	implicit val toCompanyRecord
		: TransformerF[
			ValidatedNec[String, +*],
			Company,
			CompanyRecord
			] =
		TransformerF .define[
			ValidatedNec[String, +*],
			Company,
			CompanyRecord
			]
			.withFieldRenamed (_.id, _.external_id)
			.withFieldComputed (_.created_on, _.timestamps.createdOn)
			.withFieldComputed (_.last_changed, _.timestamps.lastChanged)
			.buildTransformer
}

