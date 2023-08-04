package com.github.osxhacker.demo.chassis.domain

import java.util.UUID

import eu.timepit.refined.api.{
	Refined,
	RefinedType
	}

import io.scalaland.chimney.TransformerF

import com.github.osxhacker.demo.chassis.domain.{
	ChimneyErrors,
	Slug
	}

import com.github.osxhacker.demo.chassis.domain.entity.{
	Identifier,
	Version
	}

import com.github.osxhacker.demo.chassis.domain.event.{
	Region,
	ServiceFingerprint
	}

import com.github.osxhacker.demo.chassis.monitoring.CorrelationId


/**
 * The '''ChimneyTransformers''' type defines
 * [[io.scalaland.chimney.TransformerF]]s for shared Domain Object Model types
 * as well as the commonly used [[eu.timepit.refined.api.Refined]] class.
 */
trait ChimneyTransformers
{
	/// Class Imports
	import cats.syntax.either._
	import cats.syntax.option._
	import cats.syntax.show._
	import cats.syntax.validated._


	/// Implicit Conversions
	implicit def fromCorrelationIdToUUID
		: TransformerF[ChimneyErrors, CorrelationId, UUID] =
		new TransformerF[ChimneyErrors, CorrelationId, UUID] {
			override def transform (src : CorrelationId) : ChimneyErrors[UUID] =
				src.toUuid ()
					.validNec
			}


	implicit def fromCorrelationIdTransformerF[FTP] (
		implicit rt : RefinedType.AuxT[FTP, String]
		)
		: TransformerF[ChimneyErrors, CorrelationId, FTP] =
		new TransformerF[ChimneyErrors, CorrelationId, FTP] {
			override def transform (src : CorrelationId) : ChimneyErrors[FTP] =
				rt.refine (src.show)
					.toValidatedNec
			}


	implicit def fromRefinedTransformerF[T, P]
		: TransformerF[ChimneyErrors, Refined[T, P], T] =
		new TransformerF[ChimneyErrors, Refined[T, P], T] {
			override def transform (src : Refined[T, P]) : ChimneyErrors[T] =
				src.value.validNec
			}


	implicit def fromRegionTransformerF[FTP] (
		implicit rt : RefinedType.AuxT[FTP, String]
		)
		: TransformerF[ChimneyErrors, Region, FTP] =
		new TransformerF[ChimneyErrors, Region, FTP] {
			override def transform (src : Region) : ChimneyErrors[FTP] =
				rt.refine (Region.value.get (src).value)
					.toValidatedNec
			}


	implicit def fromRegionOptionTransformerF[FTP] (
		implicit rt : RefinedType.AuxT[FTP, String]
		)
		: TransformerF[ChimneyErrors, Option[Region], Option[FTP]] =
		new TransformerF[ChimneyErrors, Option[Region], Option[FTP]]
		{
			override def transform (src : Option[Region])
				: ChimneyErrors[Option[FTP]] =
				src.fold (none[FTP].asRight[String]) {
					r =>
						rt.refine (Region.value.get (r).value)
							.map (_.some)
					}
					.toValidatedNec
		}


	implicit def fromServiceFingerprintOptionTransformerF[FTP] (
		implicit rt : RefinedType.AuxT[FTP, String]
		)
		: TransformerF[ChimneyErrors, Option[ServiceFingerprint], Option[FTP]] =
		new TransformerF[ChimneyErrors, Option[ServiceFingerprint], Option[FTP]] {
			override def transform (src : Option[ServiceFingerprint])
				: ChimneyErrors[Option[FTP]] =
				src.fold (none[FTP].asRight[String]) {
						sf =>
							rt.refine (ServiceFingerprint.value.get (sf).value)
								.map (_.some)
					}
					.toValidatedNec
			}


	implicit def fromSlugTransformerF[FTP] (
		implicit rt : RefinedType.AuxT[FTP, String]
		)
		: TransformerF[ChimneyErrors, Slug, FTP] =
		new TransformerF[ChimneyErrors, Slug, FTP] {
			override def transform (src : Slug) : ChimneyErrors[FTP] =
				rt.refine (Slug.value.get (src).value)
					.toValidatedNec
			}


	implicit def fromVersionTransformerF[FTP] (
		implicit rt : RefinedType.AuxT[FTP, Int]
		)
		: TransformerF[ChimneyErrors, Version, FTP] =
		new TransformerF[ChimneyErrors, Version, FTP] {
			override def transform (src : Version) : ChimneyErrors[FTP] =
				rt.refine (Version.value.get (src).value)
					.toValidatedNec
			}


	implicit def fromIdentifierToRefinedString[FTP, EntityT] (
		implicit rt : RefinedType.AuxT[FTP, String]
		)
		: TransformerF[ChimneyErrors, Identifier[EntityT], FTP] =
		new TransformerF[ChimneyErrors, Identifier[EntityT], FTP] {
			override def transform (src : Identifier[EntityT])
				: ChimneyErrors[FTP] =
				rt.refine (src.toUrn ())
					.toValidatedNec
				}


	implicit def fromIdentifierToUuid[EntityT]
		: TransformerF[ChimneyErrors, Identifier[EntityT], UUID] =
		new TransformerF[ChimneyErrors, Identifier[EntityT], UUID] {
			override def transform (src : Identifier[EntityT])
				: ChimneyErrors[UUID] =
				src.toUuid ()
					.validNec
			}


	implicit def refinedToRefinedTransformerF[T, P1, P2] (
		implicit rt : RefinedType.AuxT[Refined[T, P2], T]
		)
		: TransformerF[ChimneyErrors, Refined[T, P1], Refined[T, P2]] =
		new TransformerF[ChimneyErrors, Refined[T, P1], Refined[T, P2]] {
			override def transform (src : Refined[T, P1])
				: ChimneyErrors[Refined[T, P2]] =
				rt.refine (src.value)
					.toValidatedNec
			}


	implicit def toCorrelationIdFromUUID
		: TransformerF[ChimneyErrors, UUID, CorrelationId] =
		new TransformerF[ChimneyErrors, UUID, CorrelationId] {
			override def transform (src : UUID) : ChimneyErrors[CorrelationId] =
				CorrelationId[ErrorOr] (src)
					.leftMap (_.getMessage)
					.toValidatedNec
			}


	implicit def toCorrelationIdTransformerF[P]
		: TransformerF[ChimneyErrors, Refined[String, P], CorrelationId] =
		new TransformerF[ChimneyErrors, Refined[String, P], CorrelationId] {
			override def transform (src : Refined[String, P])
				: ChimneyErrors[CorrelationId] =
				CorrelationId[ErrorOr] (src.value)
					.leftMap (_.getMessage)
					.toValidatedNec
			}


	implicit def toIdentifierFromIdT[EntityT, IdT] (
		implicit parser : Identifier.Parser[EntityT, IdT]
		)
		: TransformerF[ChimneyErrors, IdT, Identifier[EntityT]] =
		new TransformerF[ChimneyErrors, IdT, Identifier[EntityT]] {
			override def transform (src : IdT)
				: ChimneyErrors[Identifier[EntityT]] =
				parser (src).toEither
					.leftMap (_.getMessage)
					.toValidatedNec
			}


	implicit def toRefinedTransformerF[FTP, T] (
		implicit rt : RefinedType.AuxT[FTP, T]
		)
		: TransformerF[ChimneyErrors, T, FTP] =
		new TransformerF[ChimneyErrors, rt.T, FTP] {
			override def transform (src : T) : ChimneyErrors[FTP] =
				rt.refine (src)
					.toValidatedNec
			}


	implicit def toRegionTransformerF[P]
		: TransformerF[ChimneyErrors, Refined[String, P], Region] =
		new TransformerF[ChimneyErrors, Refined[String, P], Region] {
			override def transform (src : Refined[String, P])
				: ChimneyErrors[Region] =
				Region[ErrorOr] (src).leftMap (_.getMessage)
					.toValidatedNec
			}


	implicit def toRegionOptionTransformerF[P]
		: TransformerF[
			ChimneyErrors,
			Option[Refined[String, P]],
			Option[Region]
			] =
		new TransformerF[
			ChimneyErrors,
			Option[Refined[String, P]],
			Option[Region]
			] {
			override def transform (src : Option[Refined[String, P]])
				: ChimneyErrors[Option[Region]] =
				src.fold (none[Region].asRight[Throwable]) {
					rs =>
						Region[ErrorOr] (rs).map (_.some)
					}
					.leftMap (_.getMessage)
					.toValidatedNec
			}


	implicit def toServiceFingerprintOptionTransformerF[P]
		: TransformerF[
			ChimneyErrors,
			Option[Refined[String, P]],
			Option[ServiceFingerprint]
			] =
		new TransformerF[
			ChimneyErrors,
			Option[Refined[String, P]],
			Option[ServiceFingerprint]
			] {
			override def transform (src : Option[Refined[String, P]])
				: ChimneyErrors[Option[ServiceFingerprint]] =
				src.fold (none[ServiceFingerprint].asRight[Throwable]) {
					str =>
						ServiceFingerprint.from[ErrorOr] (str.value)
							.map (_.some)
					}
					.leftMap (_.getMessage)
					.toValidatedNec
			}


	implicit def toSlugTransformerF[P]
		: TransformerF[ChimneyErrors, Refined[String, P], Slug] =
		new TransformerF[ChimneyErrors, Refined[String, P], Slug] {
			override def transform (src : Refined[String, P])
				: ChimneyErrors[Slug] =
				Slug[ErrorOr] (src.value).leftMap (_.getMessage)
					.toValidatedNec
			}


	implicit def toVersionTransformerF[P]
		: TransformerF[ChimneyErrors, Refined[Int, P], Version] =
		new TransformerF[ChimneyErrors, Refined[Int, P], Version] {
			override def transform (src : Refined[Int, P])
				: ChimneyErrors[Version] =
				Version[ErrorOr] (src.value)
					.leftMap (_.getMessage)
					.toValidatedNec
			}
}

