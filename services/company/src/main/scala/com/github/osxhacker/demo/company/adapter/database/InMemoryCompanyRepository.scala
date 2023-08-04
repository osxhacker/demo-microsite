package com.github.osxhacker.demo.company.adapter.database

import scala.collection.mutable

import cats.ApplicativeThrow
import cats.effect.Sync
import fs2.Stream
import org.typelevel.log4cats.LoggerFactory

import com.github.osxhacker.demo.chassis.domain.entity.{
	Identifier,
	Version
	}

import com.github.osxhacker.demo.chassis.domain.error._
import com.github.osxhacker.demo.chassis.domain.{
	ErrorOr,
	Specification
	}

import com.github.osxhacker.demo.chassis.domain.repository._
import com.github.osxhacker.demo.company.domain.Company
import com.github.osxhacker.demo.company.domain.repository.CompanyRepository
import com.github.osxhacker.demo.company.domain.specification._


/**
 * The '''InMemoryCompanyRepository''' type defines an in-memory
 * [[com.github.osxhacker.demo.company.domain.repository.CompanyRepository]].
 * It uses `synchronized` method implementations to ensure thread safety and
 * makes no attempt to persist managed
 * [[com.github.osxhacker.demo.company.domain.Company]] instances.  Instead, it
 * is expected that an external event replay workflow will apply changes as
 * needed.
 */
final case class InMemoryCompanyRepository[F[_]] ()
	(
		implicit

		/// Needed for `pure`.
		protected val applicativeThrow : ApplicativeThrow[F],

		/// Needed for logging.
		private val loggerFactory : LoggerFactory[F],

		/// Needed for `fromIterator`.
		private val sync : Sync[F]
	)
	extends CompanyRepository[F]
{
	/// Class Imports
	import cats.syntax.all._
	import mouse.boolean._


	/// Instance Properties
	private val underlying = mutable.HashMap.empty[Identifier[Company], Company]
	val upsert = (c : Company) => replace (c).orElse (add (c))


	override def delete (instance : Company) : F[Boolean] =
		synchronized {
			underlying.remove (instance.id)
				.isDefined
				.pure[F]
			}


	override def exists (id : Identifier[Company]) : F[Option[Version]] =
		synchronized {
			underlying.get (id)
				.map (_.version)
				.pure[F]
			}


	override def find (id : Identifier[Company]) : F[Company] =
		synchronized {
			underlying.get (id)
				.toRight (ObjectNotFoundError (id))
				.liftTo[F]
			}


	override def findAll () : Stream[F, Company] =
	{
		val current = synchronized (underlying.values.toSet)

		Stream.fromIterator[F] (current.iterator, current.size)
	}


	override def save (intent : Intent[Company]) : F[Option[Company]] =
		intent match {
			case CreateIntent (unsaved) =>
				synchronized {
					(enforceUniqueProperties (unsaved) >>= add).liftTo[F]
					}

			case UpdateIntent (desired) =>
				synchronized {
					(enforceUniqueProperties (desired) >>= replace).liftTo[F]
					}

			case UpsertIntent (company) =>
				synchronized {
					(enforceUniqueProperties (company) >>= upsert).liftTo[F]
					}

			case Ignore =>
				loggerFactory.create
					.flatMap (_.debug ("ignoring company save"))
					.as (none[Company])
			}


	private def add (desired : Company) : ErrorOr[Option[Company]] =
		underlying.get (desired.id)
			.map {
				existing =>
					DuplicateObjectError[Company] (existing.id)
				}
			.toLeft (desired)
			.map {
				company =>
					underlying.put (company.id, company)
					company.some
				}


	private def enforceUniqueProperties (candidate : Company)
		: ErrorOr[Company] =
		underlying.values
			.forall (isSaveAllowed (candidate))
			.either (
				ConflictingObjectsError[Company] (
					s"an existing company conflicts with: $candidate"
					),

				candidate,
				)


	private def isSaveAllowed (candidate : Company) : Specification[Company] =
		Specification[Company] (_.id === candidate.id) || (
			!CompanyNameIs (candidate) && !CompanySlugIs (candidate)
			)


	private def replace (desired : Company) : ErrorOr[Option[Company]] =
		underlying.get (desired.id)
			.toRight (ObjectNotFoundError[Company] (desired.id))
			.filterOrElse (
				_.version === desired.version,
				StaleObjectError[Company] (desired.id, desired.version)
				)
			.flatMap (_ => desired.touch[ErrorOr] ())
			.map {
				updated =>
					underlying.put (desired.id, updated)
					updated.some
				}
}

