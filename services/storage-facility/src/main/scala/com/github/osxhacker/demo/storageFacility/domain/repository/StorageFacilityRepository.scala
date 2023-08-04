package com.github.osxhacker.demo.storageFacility.domain.repository

import com.github.osxhacker.demo.chassis.domain.repository.Repository
import com.github.osxhacker.demo.storageFacility.domain.StorageFacility


/**
 * The '''StorageFacilityRepository''' type defines the
 * [[com.github.osxhacker.demo.chassis.domain.repository.Repository]]
 * contract for managing the persistent store representation of
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]]
 * instances.
 */
trait StorageFacilityRepository[F[_]]
	extends Repository[F, StorageFacility]
{
	/**
	 * The createSchema method will create each
	 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]]
	 * related table if it does not already exist.  The returned ''Int'' is a
	 * count of how many statements were ran.
	 */
	def createSchema () : F[Int]
}

