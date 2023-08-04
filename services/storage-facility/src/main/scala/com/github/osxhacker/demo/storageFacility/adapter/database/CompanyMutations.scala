package com.github.osxhacker.demo.storageFacility.adapter.database

import doobie._
import doobie.implicits._

import schema._


/**
 * The '''CompanyMutations''' type defines common [[doobie.Fragment]]s
 * utility methods useful in defining [[doobie.Update]] and [[doobie.Update0]]
 * instances relating to persisting
 * [[com.github.osxhacker.demo.storageFacility.adapter.database.schema.CompanyRecord]]
 * instances.
 */
sealed trait CompanyMutations
	extends TableMetaData
{
	/// Class Imports
	import doobie.refined.implicits._


	protected def insert (record : CompanyRecord) : Fragment =
		sql"""
			INSERT INTO company
   			(
	  			external_id,
	  			slug,
	  			name,
	  			status
	  		)
	 		VALUES
			(
   				${record.external_id},
   				${record.slug},
	   			${record.name},
	   			${record.status}
			)
		"""
}


object CompanyMutations
	extends CompanyMutations
{
	/// Class Imports
	import doobie.refined.implicits._


	/// Class Types
	object Delete
	{
		def apply (record : CompanyRecord) : Update0 =
			sql"""
				DELETE FROM company
				WHERE
				external_id = ${record.external_id}
			"""
				.update
	}


	object Insert
	{
		def apply (record : CompanyRecord) : Update0 =
			insert (record).update
	}


	object Update
	{
		def apply (record : CompanyRecord) : Update0 =
			sql"""
		 		UPDATE company SET
					slug = ${record.slug},
					name = ${record.name},
					status = ${record.status},
					last_changed = CURRENT_TIMESTAMP
				WHERE
					external_id = ${record.external_id}
			"""
				.update
	}


	object Upsert
	{
		def apply (record : CompanyRecord) : Update0 =
			(
				insert (record) ++
				fr"""
					ON CONFLICT (external_id) DO
					UPDATE SET
						slug = EXCLUDED.slug,
						name = EXCLUDED.name,
						status = EXCLUDED.status,
						last_changed = CURRENT_TIMESTAMP
					WHERE
						company.external_id = EXCLUDED.external_id
				"""
			)
				.update
	}
}

