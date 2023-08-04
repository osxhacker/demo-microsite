package com.github.osxhacker.demo.storageFacility.adapter.database

import scala.reflect.runtime.universe.{
	TypeTag,
	typeOf
	}

import doobie._

import schema._


/**
 * The '''TableMetaData''' type defines [[doobie]]-related values for __all__
 * PostgreSQL tables known to the microservice.
 */
trait TableMetaData
{
	/// Class Imports
	import cats.syntax.option._
	import mouse.any._
	import mouse.option._


	/// Instance Properties
	protected lazy val allCompanyColumns : Fragment =
		columnsFor[CompanyRecord] ("company".some)

	protected lazy val allStorageFacilityColumns : Fragment =
		columnsFor[StorageFacilityRecord] ("storage_facility".some)


	/**
	 * The columnsFor method creates a [[doobie.Fragment]] having in
	 * declaration order __all__ properties in the ''RecordT''
	 * `primaryConstructor`.  Each is optionally prefixed with the given
	 * '''tableName'''.
	 */
	protected def columnsFor[RecordT <: Product] (tableName : Option[String])
		(implicit typeTag : TypeTag[RecordT])
		: Fragment =
		typeOf[RecordT].typeSymbol
			.asClass
			.primaryConstructor
			.asMethod
			.paramLists
			.flatten
			.map (_.name.toString)
			.map {
				column =>
					tableName.cata (
						name => s"$name.$column",
						column
					)
				}
			.mkString (", ") |>
			(Fragment.const (_))
}

