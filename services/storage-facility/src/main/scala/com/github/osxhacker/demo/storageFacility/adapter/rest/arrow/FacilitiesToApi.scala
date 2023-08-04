package com.github.osxhacker.demo.storageFacility.adapter.rest.arrow

import java.net.URI

import cats.{
	~>,
	ApplicativeThrow
	}

import cats.arrow.{
	Arrow,
	FunctionK
	}

import cats.data.{
	Kleisli,
	ValidatedNec
	}

import eu.timepit.refined
import io.circe.Json
import io.scalaland.chimney.TransformerF
import shapeless.tag
import shapeless.tag.@@
import sttp.model.{
	MediaType,
	Method
	}

import com.github.osxhacker.demo.chassis.adapter.rest.{
	Path,
	ResourceLocation,
	SemanticLink
	}

import com.github.osxhacker.demo.chassis.adapter.rest.Relation
import com.github.osxhacker.demo.chassis.adapter.rest.arrow.AbstractToApi
import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.chassis.domain.error.{
	LogicError,
	ValidationError
	}

import com.github.osxhacker.demo.storageFacility.adapter.rest.api
import com.github.osxhacker.demo.storageFacility.domain


/**
 * The '''FacilitiesToApi''' type defines the
 * [[com.github.osxhacker.demo.chassis.adapter.rest.arrow.AbstractToApi]]
 * arrow which attempts to produce a
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.api.StorageFacilities]]
 * from a ''Vector'' of
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]]
 * instances.  Each
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.api.StorageFacility]]
 * as well as the
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.api.StorageFacilities]]
 * are enriched with relevant
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.api.Links]] and
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.api.Embedded]]
 * information as applicable and requested.
 */
final case class FacilitiesToApi[F[_]] ()
	(
		implicit
		private val applicativeThrow : ApplicativeThrow[F],
		private val arrow : Arrow[Kleisli[F, *, *]],
		private val transformer : FunctionK[
			Lambda[R => Vector[domain.StorageFacility] => ErrorOr[R]],
			Kleisli[F, Vector[domain.StorageFacility], *]
			],

		private val mkApiFacility : TransformerF[
			ValidatedNec[String, +*],
			domain.StorageFacility,
			api.StorageFacility,
			]
	)
	extends AbstractToApi[
		Kleisli[F, *, *],
		ErrorOr,
		Vector[domain.StorageFacility],
		api.StorageFacilities
		] ()
{
	/// Class Imports
	import FacilitiesToApi._
	import api.StorageFacilities.Optics.{
		_links,
		facilities
		}

	import cats.syntax.option._
	import cats.syntax.show._
	import cats.syntax.traverse._
	import refined.cats._


	/// Instance Properties
	override protected val addLinks =
		Kleisli[
			ErrorOr,
			(
				Vector[domain.StorageFacility],
				api.StorageFacilities,
				ResourceLocation @@ api.StorageFacilities
			),

			api.StorageFacilities
			] {
			case (facilities, resources, location) =>
				_links.replace (
					api.Links () (
						api.Links.AdditionalProperties (
							mkLinks (location).foldLeft (Map.empty[String, Json]) {
								case (accum, link) =>
									accum ++ link.toMap ()
								}
							)
							.some
						)
						.some
					)
					.andThen (
						addFacilityLinks (location, mkFacilitiesMap (facilities))
						) (resources)
			}
			.mapK[F] (implicitly[ErrorOr ~> F])

	override protected lazy val factory =
		_.traverse {
			mkApiFacility.transform (_)
				.leftMap (ValidationError[domain.StorageFacility])
				.toEither
			}
			.flatMap {
				items =>
					api.StorageFacilities
						.from (facilities = items)
				}


	private def addFacilityLinks (
		parent : ResourceLocation,
		knownFacilities : Map[String, domain.StorageFacility]
		)
		: api.StorageFacilities => ErrorOr[api.StorageFacilities] =
		facilities.each.modifyA[ErrorOr] {
			resource =>
				for {
					facility <- knownFacilities.get (resource.id.value)
						.toRight (
							LogicError (
								s"unable to resolve facility for: ${resource.id.show}"
								)
							)

					subpath <- Path.from[ErrorOr, String] (
						s"/${facility.id.toUuid ().toString}"
						)
					} yield FacilityToApi.defineLinksFor (
						tag[api.StorageFacility] (parent / subpath)
						) (facility, resource)
			}


	private def mkFacilitiesMap (facilities : Vector[domain.StorageFacility])
		: Map[String, domain.StorageFacility] =
		facilities.map {
			facility =>
				facility.id.toUrn () -> facility
			}
			.toMap
}


object FacilitiesToApi
	extends AbstractToApi.Companion[domain.StorageFacility] ()
{
	/// Class Imports
	import refined.auto._


	/// Class Types
	final case class CreateFacility (override val href : URI)
		extends SemanticLink (
			method = Method.POST,
			href = href,
			rel = Relation (commandUrn ("create")),
			mediaType = MediaType.ApplicationJson,
			)


	private def mkLinks (location : ResourceLocation @@ api.StorageFacilities)
		: List[SemanticLink] =
		SemanticLink.Self (location.toUri ()) ::
		CreateFacility (location.toUri ()) ::
		Nil
}

