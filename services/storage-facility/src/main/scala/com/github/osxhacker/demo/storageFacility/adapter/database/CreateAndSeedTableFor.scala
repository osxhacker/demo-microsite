package com.github.osxhacker.demo.storageFacility.adapter.database

import scala.io.Source
import scala.reflect.ClassTag

import doobie.util.update.Update0

import com.github.osxhacker.demo.chassis.domain.ErrorOr


/**
 * THe '''CreateAndSeedTableFor''' `object` defines the algorithm for
 * initializing the persistent representation of a ''RecordT''.  The DDL is
 * expected to be able to be safely executed each time it is evaluated, without
 * losing existing information in the persistent store.
 */
object CreateAndSeedTableFor
{
	/// Class Imports
	import mouse.any._


	/// Instance Properties
	private val endOfStatement = "\u0003"
	private val sqlComment = """^--.*$"""


	def apply[RecordT <: Product] ()
		(implicit classTag : ClassTag[RecordT])
		: ErrorOr[Seq[Update0]] =
		sqlFor[RecordT] () |> load |> generate[RecordT]


	private def generate[RecordT] (source : Option[Source])
		(implicit classTag : ClassTag[RecordT])
		: ErrorOr[Seq[Update0]] =
		source.toRight (
			new IllegalArgumentException (
				s"unable to find SQL for ${classTag.runtimeClass.getSimpleName}"
				)
			)
			.map (parseSource)


	private def load (location : String) : Option[Source] =
		Option (getClass.getResource (location))
			.map (Source.fromURL)


	private def parseSource (source : Source) : Seq[Update0] =
		source.getLines ()
			.map (_.trim)
			.filterNot (_.matches (sqlComment))
			.map (_.replaceAll (""";$""", endOfStatement))
			.filterNot (_.isEmpty)
			.mkString (" ")
			.split (endOfStatement)
			.toIndexedSeq
			.map (Update0 (_, None))


	private def sqlFor[RecordT] ()
		(implicit classTag : ClassTag[RecordT])
		: String =
		s"/${classTag.runtimeClass.getSimpleName}.sql"
}

