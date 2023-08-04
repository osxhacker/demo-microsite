package com.github.osxhacker.demo.storageFacility.adapter.rest.arrow

import cats.Applicative
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
import shapeless.{
	Path => _,
	syntax => _,
	_
	}

import shapeless.tag.@@

import com.github.osxhacker.demo.chassis.adapter.rest.{
	Path,
	Relation,
	ResourceLocation,
	SemanticLink
	}

import com.github.osxhacker.demo.chassis.adapter.rest.arrow.AbstractToApi
import com.github.osxhacker.demo.storageFacility.adapter.rest.api
import com.github.osxhacker.demo.storageFacility.domain
import com.github.osxhacker.demo.storageFacility.domain.specification.{
	CompanyIsActive,
	FacilityStatusCanBecome,
	FacilityStatusIs
	}


/**
 * The '''FacilityToApi''' type defines the
 * [[com.github.osxhacker.demo.chassis.adapter.rest.arrow.AbstractToApi]]
 * arrow which attempts to produce a
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.api.StorageFacility]]
 * from a
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]].  The
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.api.StorageFacility]]
 * is enriched with relevant
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.api.Links]] and
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.api.Embedded]]
 * information as applicable and requested.
 */
final case class FacilityToApi[F[_]] ()
	(
		implicit
		private val applicative : Applicative[F],
		private val arrow : Arrow[Kleisli[F, *, *]],
		private val chimney : TransformerF[
			ValidatedNec[String, +*],
			domain.StorageFacility,
			api.StorageFacility
			],

		private val transformer : FunctionK[
			Lambda[R => domain.StorageFacility => ValidatedNec[String, R]],
			Kleisli[F, domain.StorageFacility, *]
			]
	)
	extends AbstractToApi[
		Kleisli[F, *, *],
		ValidatedNec[String, +*],
		domain.StorageFacility,
		api.StorageFacility
		] ()
{
	/// Class Imports
	import FacilityToApi.defineLinksFor
	import cats.syntax.applicative._


	/// Instance Properties
	override protected lazy val addLinks = Kleisli {
		case (facility, resource, location) =>
			defineLinksFor (location) (facility, resource).pure[F]
		}

	override protected val factory = chimney.transform
}


object FacilityToApi
	extends AbstractToApi.Companion[domain.StorageFacility] ()
{
	/// Class Imports
	import api.StorageFacility.Optics._links
	import cats.syntax.option._
	import refined.auto._


	/// Class Types
	object StatusSemantics
		extends Poly2
	{
		/// Class Types
		type ResultType[StatusT <: domain.StorageFacilityStatus] = Case.Aux[
			StatusT,
			ResourceLocation @@ api.StorageFacility,
			Option[SemanticLink]
			]


		/// Implicit Conversions
		implicit val caseActive
			: ResultType[domain.StorageFacilityStatus.Active.type] =
			at {
				case (_, location) =>
					ChangeStatus (
						(location / Path ("/activate")).toUri (),
						Relation (commandUrn ("activate"))
						)
						.some
				}

		implicit val caseClosed
			: ResultType[domain.StorageFacilityStatus.Closed.type] =
			at {
				case (_, location) =>
					ChangeStatus (
						(location / Path ("/close")).toUri (),
						Relation (commandUrn ("close"))
						)
						.some
				}

		implicit val caseUnderConstruction
			: ResultType[domain.StorageFacilityStatus.UnderConstruction.type] =
			at {
				case (_, _) =>
					none[SemanticLink]
				}
	}


	/// Instance Properties
	private val companyIsActive = CompanyIsActive (
		domain.StorageFacility
			.owner
		)

	private val noLinks = Map.empty[String, Json]


	/**
	 * The defineLinksFor method defines an [[cats.Endo]] which replaces __all__
	 * [[com.github.osxhacker.demo.chassis.adapter.rest.SemanticLink]]s in a
	 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.api.StorageFacility]]
	 * with those suitable for it based on its state.
	 */
	def defineLinksFor (location : ResourceLocation @@ api.StorageFacility)
		(facility : domain.StorageFacility, resource : api.StorageFacility)
		: api.StorageFacility =
		_links.replace {
			api.Links () (
				api.Links.AdditionalProperties (
					mkLinks (facility, location).foldLeft (noLinks) {
						case (accum, link) =>
							accum ++ link.toMap ()
						}
					)
					.some
				)
				.some
			} (resource)


	private def mkChangeStatusLinks (
		facility : domain.StorageFacility,
		location : ResourceLocation @@ api.StorageFacility
		)
		: List[SemanticLink] =
		domain.StorageFacilityStatus
			.values
			.filter {
				status =>
					(
						!FacilityStatusIs (status) &&
						FacilityStatusCanBecome (status)
					)
					.isSatisfiedBy (facility)
				}
			.flatMap {
				_.pfmap[
					StatusSemantics.type,
					ResourceLocation @@ api.StorageFacility,
					Option[SemanticLink]
					] (location)
				}
			.toList


	private def mkLinks (
		facility : domain.StorageFacility,
		location : ResourceLocation @@ api.StorageFacility
		)
		: List[SemanticLink] =
		SemanticLink.Self (location.toUri ()) ::
		SemanticLink.Delete (location.toUri ()) ::
		mkChangeStatusLinks (facility, location) :::
		facility.when (companyIsActive) {
			_ =>
				SemanticLink.Edit (location.toUri ())
			}
			.toList
}

