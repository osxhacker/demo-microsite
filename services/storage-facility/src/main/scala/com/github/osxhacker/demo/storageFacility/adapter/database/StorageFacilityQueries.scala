package com.github.osxhacker.demo.storageFacility.adapter.database

import java.util.UUID

import doobie._
import doobie.implicits._
import shapeless.{
	syntax => _,
	_
	}

import com.github.osxhacker.demo.chassis.domain.entity.Version

import schema._


/**
 * The '''StorageFacilityQueries''' type defines common [[doobie.Fragment]]s and
 * utility methods useful in defining [[doobie.Query]] and [[doobie.Query0]]
 * instances relating to querying for
 * [[com.github.osxhacker.demo.storageFacility.adapter.database.schema.StorageFacilityRecord]]
 * instances.
 */
sealed trait StorageFacilityQueries
	extends TableMetaData
{
	/// Class Imports
	import doobie.implicits.legacy.instant._
	import doobie.refined.implicits._


	/// Class Types
	type QueryResultsType = StorageFacilityRecord :: CompanyRecord :: HNil


	/// Instance Properties
	protected val selectAll : Fragment =
		sql"""
			SELECT $allStorageFacilityColumns,
				$allCompanyColumns
			FROM storage_facility,
				company
			WHERE company.key = storage_facility.company_key
		"""


	protected def selectAllById (id : UUID) : Query0[QueryResultsType] =
		(
			selectAll ++
			fr"""
				AND storage_facility.external_id = $id
			"""
		)
			.query[QueryResultsType]
}


object StorageFacilityQueries
	extends StorageFacilityQueries
{
	/// Class Imports
	import doobie.implicits.legacy.instant._
	import doobie.refined.implicits._


	/// Class Types
	object Exists
	{
		def apply (id : UUID) : ConnectionIO[Option[Version]] =
			sql"""
				SELECT storage_facility.version
				FROM storage_facility
				WHERE storage_facility.external_id = $id
			"""
				.query[Version]
				.option
	}


	object FindAll
	{
		def apply () : fs2.Stream[ConnectionIO, QueryResultsType] =
			selectAll.query[QueryResultsType]
			.stream
	}


	object FindById
	{
		def apply (id : UUID) : ConnectionIO[Option[QueryResultsType]] =
			selectAllById (id).option
	}


	object Refresh
	{
		def apply (id : UUID) : ConnectionIO[QueryResultsType] =
			selectAllById (id).unique
	}
}

