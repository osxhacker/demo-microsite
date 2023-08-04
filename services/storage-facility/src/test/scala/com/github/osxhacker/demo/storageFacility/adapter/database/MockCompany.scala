package com.github.osxhacker.demo.storageFacility.adapter.database

import scala.collection.mutable

import cats.ApplicativeThrow
import cats.effect.Sync
import fs2.Stream

import com.github.osxhacker.demo.chassis.domain.entity.{
	Identifier,
	Version
	}

import com.github.osxhacker.demo.chassis.domain.error.{
	DomainValueError,
	DuplicateObjectError,
	ObjectNotFoundError
	}

import com.github.osxhacker.demo.chassis.domain.repository._
import com.github.osxhacker.demo.storageFacility.domain.{
	Company,
	CompanyReference
	}

import com.github.osxhacker.demo.storageFacility.domain.repository.CompanyRepository


/**
 * The '''MockCompany''' type satisfies the
 * [[com.github.osxhacker.demo.storageFacility.domain.repository.CompanyRepository]]
 * contract without having external dependencies on a persistent store.  If
 * non-default behaviour is desired, it is the responsibiity of the implementor
 * to ensure `synchronized` access to the `underlying`
 * [[scala.collection.mutable.Map]].
 */
class MockCompany[F[_]] (
	private val underlying : mutable.Map[Identifier[Company], Company]
	)
	(
		implicit

		/// Needed for `pure`.
		protected val applicativeThrow : ApplicativeThrow[F],

		/// Needed for `fromIterator`.
		private val sync : Sync[F]
	)
	extends CompanyRepository[F]
{
	/// Class Imports
	import cats.syntax.all._
	import mouse.option._


	def this ()
		(
			implicit
			applicativeThrow : ApplicativeThrow[F],
			sync : Sync[F]
		)
		=
		this (mutable.HashMap.empty[Identifier[Company], Company])


	def this (company : Company)
		(
			implicit
			applicativeThrow : ApplicativeThrow[F],
			sync : Sync[F]
		)
		=
		this (mutable.HashMap (company.id -> company))


	override def createSchema () : F[Int] = 0.pure[F]


	override def find (reference : CompanyReference) : F[Company] =
		synchronized {
			underlying.values
				.filter (_.toRef () === reference)
				.headOption
				.toRight (DomainValueError (s"unknown reference: $reference"))
				.liftTo[F]
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

		Stream.fromIterator[F](current.iterator, current.size)
	}


	override def delete (instance : Company) : F[Boolean] =
		synchronized {
			underlying.remove (instance.id)
				.isDefined
				.pure[F]
			}


	override def exists (id : Identifier[Company])
		: F[Option[Version]] =
		synchronized {
			underlying
				.get (id)
				.as (Version.initial)
				.pure[F]
			}


	override def save (intent : Intent[Company])
		: F[Option[Company]] =
		synchronized {
			intent match {
				case Ignore =>
					none[Company].pure[F]

				case CreateIntent (company) if underlying.contains (company.id) =>
					DuplicateObjectError (company.id)
						.raiseError[F, Option[Company]]

				case CreateIntent (company) =>
					underlying.put (company.id, company)
						.orElse (company.some)
						.pure[F]

				case UpdateIntent (company) =>
					underlying.put (company.id, company)
						.as (company)
						.pure[F]

				case UpsertIntent (company) =>
					underlying.put (company.id, company)

					company.some
						.pure[F]
				}
			}
}

