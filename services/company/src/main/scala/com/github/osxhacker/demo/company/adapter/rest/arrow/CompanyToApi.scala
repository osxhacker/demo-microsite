package com.github.osxhacker.demo.company.adapter.rest.arrow

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

import com.github.osxhacker.demo.company.adapter.rest.api
import com.github.osxhacker.demo.chassis.adapter.rest.arrow.AbstractToApi
import com.github.osxhacker.demo.company.domain
import com.github.osxhacker.demo.company.domain.specification.{
	CompanyStatusCanBecome,
	CompanyStatusIs
	}


/**
 * The '''CompanyToApi''' type defines the
 * [[com.github.osxhacker.demo.chassis.adapter.rest.arrow.AbstractToApi]]
 * arrow which attempts to produce a
 * [[com.github.osxhacker.demo.company.adapter.rest.api.Company]]
 * from a
 * [[com.github.osxhacker.demo.company.domain.Company]].  The
 * [[com.github.osxhacker.demo.company.adapter.rest.api.Company]]
 * is enriched with relevant
 * [[com.github.osxhacker.demo.company.adapter.rest.api.Links]] and
 * [[com.github.osxhacker.demo.company.adapter.rest.api.Embedded]]
 * information as applicable and requested.
 */
final case class CompanyToApi[F[_]] ()
	(
		implicit
		private val applicative : Applicative[F],
		private val arrow : Arrow[Kleisli[F, *, *]],
		private val chimney : TransformerF[
			ValidatedNec[String, +*],
			domain.Company,
			api.Company
			],

		private val transformer : FunctionK[
			Lambda[R => domain.Company => ValidatedNec[String, R]],
			Kleisli[F, domain.Company, *]
			]
	)
	extends AbstractToApi[
		Kleisli[F, *, *],
		ValidatedNec[String, +*],
		domain.Company,
		api.Company
		] ()
{
	/// Class Imports
	import CompanyToApi.defineLinksFor
	import cats.syntax.applicative._


	/// Instance Properties
	override protected val addLinks = Kleisli {
		case (company, resource, location) =>
			defineLinksFor (location) (company, resource).pure[F]
		}

	override protected val factory = chimney.transform
}


object CompanyToApi
	extends AbstractToApi.Companion[domain.Company] ()
{
	/// Class Imports
	import api.Company.Optics._links
	import cats.syntax.option._
	import refined.auto._


	/// Class Types
	object StatusSemantics
		extends Poly2
	{
		/// Class Types
		type ResultType[StatusT <: domain.CompanyStatus] = Case.Aux[
			StatusT,
			ResourceLocation @@ api.Company,
			SemanticLink
			]


		/// Implicit Conversions
		implicit val caseActive : ResultType[domain.CompanyStatus.Active.type] =
			at {
				case (_, location) =>
					ChangeStatus (
						(location / Path ("/activate")).toUri (),
						Relation (commandUrn ("activate"))
						)
				}

		implicit val caseInactive
			: ResultType[domain.CompanyStatus.Inactive.type] =
			at {
				case (_, location) =>
					ChangeStatus (
						(location / Path ("/deactivate")).toUri (),
						Relation (commandUrn ("deactivate"))
						)
				}

		implicit val caseSuspended
			: ResultType[domain.CompanyStatus.Suspended.type] =
			at {
				case (_, location) =>
					ChangeStatus (
						(location / Path ("/suspend")).toUri (),
						Relation (commandUrn ("suspend"))
						)
				}
	}


	/// Instance Properties
	private val noLinks = Map.empty[String, Json]


	/**
	 * The defineLinksFor method defines the algorithm which replaces __all__
	 * [[com.github.osxhacker.demo.chassis.adapter.rest.SemanticLink]]s in a
	 * [[com.github.osxhacker.demo.company.adapter.rest.api.Company]]
	 * with those suitable for it based on its state.
	 */
	def defineLinksFor (location : ResourceLocation @@ api.Company)
		(company : domain.Company, resource : api.Company)
		: api.Company =
		_links.replace {
			api.Links () (
				api.Links.AdditionalProperties (
					mkLinks (company, location).foldLeft (noLinks) {
						case (accum, link) =>
							accum ++ link.toMap ()
						}
					)
					.some
				)
				.some
		} (resource)


	private def mkChangeStatusLinks (
		company : domain.Company,
		location : ResourceLocation @@ api.Company
		)
		: List[SemanticLink] =
		domain.CompanyStatus
			.values
			.filter {
				status =>
					(
						!CompanyStatusIs (status) &&
						CompanyStatusCanBecome (status)
					)
					.isSatisfiedBy (company)
				}
			.map {
				_.pfmap[
					StatusSemantics.type,
					ResourceLocation @@ api.Company,
					SemanticLink
					] (location)
				}
			.toList


	private def mkLinks (
		company : domain.Company,
		location : ResourceLocation @@ api.Company
		)
		: List[SemanticLink] =
		SemanticLink.Self (location.toUri ()) ::
		SemanticLink.Edit (location.toUri ()) ::
		SemanticLink.Delete (location.toUri ()) ::
		mkChangeStatusLinks (company, location)
}

