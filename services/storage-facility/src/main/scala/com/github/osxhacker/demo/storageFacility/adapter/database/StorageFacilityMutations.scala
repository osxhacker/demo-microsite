package com.github.osxhacker.demo.storageFacility.adapter.database

import doobie._
import doobie.implicits._

import schema._


/**
 * The '''StorageFacilityMutations''' type defines common [[doobie.Fragment]]s
 * utility methods useful in defining [[doobie.Update]] and [[doobie.Update0]]
 * instances relating to persisting
 * [[com.github.osxhacker.demo.storageFacility.adapter.database.schema.StorageFacilityRecord]]
 * instances.
 */
sealed trait StorageFacilityMutations
	extends TableMetaData
{
}


object StorageFacilityMutations
	extends StorageFacilityMutations
{
	/// Class Imports
	import doobie.implicits.legacy.instant._
	import doobie.refined.implicits._


	/// Class Types
	object Delete
	{
		def apply (record : StorageFacilityRecord) : Update0 =
			sql"""
				DELETE FROM storage_facility
				WHERE
				external_id = ${record.external_id}
				AND version = ${record.version}
			"""
				.update
	}


	object Insert
	{
		def apply (record : StorageFacilityRecord) : Update0 =
			sql (record).update


		def sql (record : StorageFacilityRecord) : Fragment =
			sql"""
				INSERT INTO storage_facility
				(
					external_id,
					version,
					company_key,
	 				region,
					status,
					name,
					city,
					state,
					zip,
					capacity,
					available
				)
				VALUES
				(
					${record.external_id},
					${record.version},
					${record.company_key},
	 				${record.region},
					${record.status},
					${record.name},
					${record.city},
					${record.state},
					${record.zip},
					${record.capacity},
					${record.available}
				)
		   """
	}


	object Update
	{
		def apply (record : StorageFacilityRecord) : Update0 =
			sql"""
				UPDATE storage_facility SET
					version = ${record.version} + 1,
					region = ${record.region},
					status = ${record.status},
					name = ${record.name},
					city = ${record.city},
					state = ${record.state},
					zip = ${record.zip},
					capacity = ${record.capacity},
					available = ${record.available},
					last_changed = CURRENT_TIMESTAMP
				WHERE
					external_id = ${record.external_id} AND
					version = ${record.version}
		   """
				.update
	}


	object Upsert
	{
		def apply (record : StorageFacilityRecord) : Update0 =
			(
				Insert.sql (record) ++
				fr"""
					ON CONFLICT (external_id) DO
					UPDATE SET
						version = EXCLUDED.version + 1,
						region = EXCLUDED.region,
						status = EXCLUDED.status,
						name = EXCLUDED.name,
						city = EXCLUDED.city,
						state = EXCLUDED.state,
						zip = EXCLUDED.zip,
						capacity = EXCLUDED.capacity,
						available = EXCLUDED.available,
						last_changed = CURRENT_TIMESTAMP
					WHERE
						storage_facility.external_id = EXCLUDED.external_id AND
						storage_facility.version = EXCLUDED.version
				  """
			)
				.update
	}
}
