package com.github.osxhacker.demo.storageFacility.adapter

import java.util.UUID

import cats.Show
import cats.data.{
	NonEmptyList,
	ValidatedNec
	}

import enumeratum.{
	Enum,
	EnumEntry
	}

import io.scalaland.chimney.TransformerF

import com.github.osxhacker.demo.chassis.domain.{
	ErrorOr,
	Slug
	}

import com.github.osxhacker.demo.chassis.domain.entity.Version
import com.github.osxhacker.demo.chassis.domain.event.Region


/**
 * The '''database''' `package` contains types responsible for managing the
 * persistent representations of
 * [[com.github.osxhacker.demo.storageFacility.domain]] model concepts.  This is
 * done in part by using the RAML
 * [[com.github.osxhacker.demo.storageFacility.adapter.database.schema]]
 * generated types.
 */
package object database
{
	/// Class Imports
	import cats.syntax.either._
	import cats.syntax.validated._
	import doobie._


	/// Implicit Conversions
	implicit val slugGet : Get[Slug] =
		Get[String].temap (Slug[ErrorOr] (_).leftMap (_.getMessage))

	implicit val slugPut : Put[Slug] =
		Put[String].tcontramap (Slug.value.get (_).value)

	implicit val regionGet : Get[Region] =
		Get[String].temap {
			candidate =>
				Region.Value
					.from (candidate)
					.map (Region (_))
			}

	implicit val regionPut : Put[Region] =
		Put[String].tcontramap (Region.value.get (_).value)

	implicit val uuidGet : Get[UUID] =
		Get.Advanced.other[UUID] (NonEmptyList.of ("uuid"))

	implicit val uuidPut : Put[UUID] =
		Put.Advanced.other[UUID] (NonEmptyList.of ("uuid"))

	implicit val uuidShow : Show[UUID] = Show.show (_.toString)

	implicit val versionGet : Get[Version] =
		Get[Int].temap (Version[ErrorOr] (_).leftMap (_.getMessage))

	implicit val versionPut : Put[Version] =
		Put[Int].tcontramap (Version.value.get (_).value)


	implicit def enumEntryToString[A <: EnumEntry]
		: TransformerF[ValidatedNec[String, +*], A, String] =
		new TransformerF[ValidatedNec[String, +*], A, String] {
			override def transform (src : A)
				: ValidatedNec[String, String] =
				src.entryName
					.validNec
			}


	implicit def intToPrimaryKey[RecordT <: Product]
		: Get[PrimaryKey[RecordT]] =
		Get[Int].map (PrimaryKey[RecordT])


	implicit def primaryKeyToInt[RecordT <: Product]
		: Put[PrimaryKey[RecordT]] =
		Put[Int].tcontramap (_.key)


	implicit def stringToEnumEntry[A <: EnumEntry] (
		implicit materialized : Enum[A]
		)
		: TransformerF[ValidatedNec[String, +*], String, A] =
		new TransformerF[ValidatedNec[String, +*], String, A] {
			override def transform (src : String)
				: ValidatedNec[String, A] =
				materialized.withNameInsensitiveEither (src)
					.leftMap {
						notFound =>
							new StringBuilder ()
								.append ("'")
								.append (notFound.notFoundName)
								.append ("' not found in: ")
								.append (notFound.enumValues)
								.toString ()
					}
					.toValidatedNec
			}
	}

