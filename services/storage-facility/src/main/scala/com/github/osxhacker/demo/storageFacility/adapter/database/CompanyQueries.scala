package com.github.osxhacker.demo.storageFacility.adapter.database

import java.util.UUID

import doobie._
import doobie.implicits._

import com.github.osxhacker.demo.chassis.domain.Slug
import com.github.osxhacker.demo.chassis.domain.entity.Version

import schema._


/**
 * The '''CompanyQueries''' type defines common [[doobie.Fragment]]s and
 * utility methods useful in defining [[doobie.Query]] and [[doobie.Query0]]
 * instances relating to querying for
 * [[com.github.osxhacker.demo.storageFacility.adapter.database.schema.CompanyRecord]]
 * instances.
 */
sealed trait CompanyQueries
	extends TableMetaData
{
	/// Class Imports
	import doobie.implicits.legacy.instant._
	import doobie.refined.implicits._


	/// Instance Properties
	protected val selectAll : Fragment =
		sql"""
			SELECT $allCompanyColumns
			FROM company
		"""


	protected def selectAllById (id : UUID) : Query0[CompanyRecord] =
		(
			selectAll ++
				fr"""
					WHERE company.external_id = $id
				"""
			)
			.query[CompanyRecord]
}


object CompanyQueries
	extends CompanyQueries
{
	/// Class Imports
	import doobie.Fragments.whereAnd
	import doobie.implicits.legacy.instant._
	import doobie.refined.implicits._


	/// Class Types
	object Exists
	{
		def apply (id : UUID) : ConnectionIO[Option[Version]] =
			sql"""
				SELECT ${Version.initial}
				FROM company
				WHERE company.external_id = $id
			"""
				.query[Version]
				.option
	}


	object FindAll
	{
		def apply () : fs2.Stream[ConnectionIO, CompanyRecord] =
			selectAll.query[CompanyRecord]
				.stream
	}


	object FindById
	{
		def apply (id : UUID) : ConnectionIO[Option[CompanyRecord]] =
			selectAllById (id).option
	}


	object FindBySlug
	{
		def apply (slug : Slug) : ConnectionIO[Option[CompanyRecord]] =
			(
				selectAll ++
				whereAnd (fr"company.slug = $slug")
			)
				.query[CompanyRecord]
				.option
	}


	object FindKeyById
	{
		def apply (id : UUID) : ConnectionIO[PrimaryKey[CompanyRecord]] =
			sql"""
				SELECT key
				FROM company
				WHERE external_id = $id
			"""
				.query[PrimaryKey[CompanyRecord]]
				.unique
	}


	object Refresh
	{
		def apply (id : UUID) : ConnectionIO[CompanyRecord] =
			(
				selectAll ++
				whereAnd (fr"company.external_id = $id")
			)
				.query[CompanyRecord]
				.unique
	}
}

