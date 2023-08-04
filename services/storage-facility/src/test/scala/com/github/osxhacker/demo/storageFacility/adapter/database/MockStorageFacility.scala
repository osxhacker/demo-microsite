package com.github.osxhacker.demo.storageFacility.adapter.database

import scala.collection.mutable

import cats.ApplicativeThrow
import cats.effect.Sync
import fs2.Stream

import com.github.osxhacker.demo.chassis.domain.entity.{
	Identifier,
	Version
	}

import com.github.osxhacker.demo.chassis.domain.error.ObjectNotFoundError
import com.github.osxhacker.demo.chassis.domain.repository.Intent
import com.github.osxhacker.demo.storageFacility.domain.StorageFacility
import com.github.osxhacker.demo.storageFacility.domain.repository.StorageFacilityRepository


/**
 * The '''MockStorageFacility''' type satisfies the
 * [[com.github.osxhacker.demo.storageFacility.domain.repository.StorageFacilityRepository]]
 * contract without having external dependencies on a persistent store.  If
 * non-default behaviour is desired, it is the responsibiity of the implementor
 * to ensure `synchronized` access to the `underlying`
 * [[scala.collection.mutable.Map]].
 */
class MockStorageFacility[F[_]] (
	private val underlying : mutable.Map[
		Identifier[StorageFacility],
		StorageFacility
		]
	)
	(
		implicit
		/// Needed for `pure`.
		protected val applicativeThrow : ApplicativeThrow[F],

		/// Needed for `fromIterator`.
		private val sync : Sync[F]
	)
	extends StorageFacilityRepository[F]
{
	/// Class Imports
	import cats.syntax.applicative._
	import cats.syntax.either._
	import cats.syntax.functor._
	import cats.syntax.option._


	def this ()
		(
			implicit
			applicativeThrow : ApplicativeThrow[F],
			sync : Sync[F]
		)
		=
		this (mutable.HashMap.empty[Identifier[StorageFacility], StorageFacility])


	def this (facility : StorageFacility)
		(
			implicit
			applicativeThrow : ApplicativeThrow[F],
			sync : Sync[F]
		)
		=
		this (mutable.HashMap (facility.id -> facility))


	override def createSchema () : F[Int] = 0.pure[F]


	override def find (id : Identifier[StorageFacility]) : F[StorageFacility] =
		synchronized {
			underlying.get (id)
				.toRight (ObjectNotFoundError (id))
				.liftTo[F]
			}


	override def findAll () : Stream[F, StorageFacility] =
	{
		val current = synchronized (underlying.values.toSet)

		Stream.fromIterator[F] (current.iterator, current.size)
	}


	override def delete (instance : StorageFacility) : F[Boolean] =
		synchronized {
			underlying.remove (instance.id)
				.isDefined
				.pure[F]
			}


	override def exists (id : Identifier[StorageFacility])
		: F[Option[Version]] =
		synchronized {
			underlying.get (id)
				.map (_.version)
				.pure[F]
			}


	override def save (intent : Intent[StorageFacility])
		: F[Option[StorageFacility]] =
			intent.fold (
				none[StorageFacility].pure[F],
				_.touch[F] ()
					.map {
						updated =>
							synchronized {
								underlying.put (updated.id, updated)
								}

							updated.some
						}
				)
}

