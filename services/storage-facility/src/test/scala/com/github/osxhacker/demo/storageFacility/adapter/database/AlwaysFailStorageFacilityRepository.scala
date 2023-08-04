package com.github.osxhacker.demo.storageFacility.adapter.database

import cats.ApplicativeThrow
import com.github.osxhacker.demo.chassis.domain.entity.{Identifier, Version}
import com.github.osxhacker.demo.chassis.domain.repository.Intent
import com.github.osxhacker.demo.storageFacility.domain.StorageFacility
import com.github.osxhacker.demo.storageFacility.domain.repository.StorageFacilityRepository


/**
 * The '''AlwaysFailStorageFacilityRepository''' type fulfills the
 * [[com.github.osxhacker.demo.storageFacility.domain.repository.StorageFacilityRepository]]
 * by raising an `error` within the context ''F[_]''.
 */
final case class AlwaysFailStorageFacilityRepository[F[_]] (
	val error : Throwable
	)
	(implicit private val applicativeThrow : ApplicativeThrow[F])
	extends StorageFacilityRepository[F]
{
	override def createSchema () : F[Int] = failure ()


	override def delete (instance : StorageFacility) : F[Boolean] = failure ()


	override def exists (id : Identifier[StorageFacility])
		: F[Option[Version]] =
		failure ()


	override def find (id : Identifier[StorageFacility]) : F[StorageFacility] =
		failure ()


	override def findAll () : fs2.Stream[F, StorageFacility] =
		fs2.Stream.raiseError (error)


	override def save (intent : Intent[StorageFacility])
		: F[Option[StorageFacility]] =
		failure ()


	private def failure[A] () = applicativeThrow.raiseError[A] (error)
}
