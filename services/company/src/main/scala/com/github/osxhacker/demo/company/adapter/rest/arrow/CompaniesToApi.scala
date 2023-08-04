package com.github.osxhacker.demo.company.adapter.rest.arrow

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

import com.github.osxhacker.demo.company.adapter.rest.api
import com.github.osxhacker.demo.company.domain


/**
 * The '''CompaniesToApi''' type defines the
 * [[com.github.osxhacker.demo.chassis.adapter.rest.arrow.AbstractToApi]]
 * arrow which attempts to produce a
 * [[com.github.osxhacker.demo.company.adapter.rest.api.Companies]]
 * from a ''Vector'' of
 * [[com.github.osxhacker.demo.company.domain.Company]]
 * instances.  Each
 * [[com.github.osxhacker.demo.company.adapter.rest.api.Company]]
 * as well as the
 * [[com.github.osxhacker.demo.company.adapter.rest.api.Companies]]
 * are enriched with relevant
 * [[com.github.osxhacker.demo.company.adapter.rest.api.Links]] and
 * [[com.github.osxhacker.demo.company.adapter.rest.api.Embedded]]
 * information as applicable and requested.
 */
final case class CompaniesToApi[F[_]] ()
	(
		implicit
		private val applicativeThrow : ApplicativeThrow[F],
		private val arrow : Arrow[Kleisli[F, *, *]],
		private val transformer : FunctionK[
			Lambda[R => Vector[domain.Company] => ErrorOr[R]],
			Kleisli[F, Vector[domain.Company], *]
			],

		private val mkApiCompany : TransformerF[
			ValidatedNec[String, +*],
			domain.Company,
			api.Company,
			]
	)
	extends AbstractToApi[
		Kleisli[F, *, *],
		ErrorOr,
		Vector[domain.Company],
		api.Companies
		] ()
{
	/// Class Imports
	import CompaniesToApi._
	import api.Companies.Optics.{
		_links,
		companies
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
				Vector[domain.Company],
				api.Companies,
				ResourceLocation @@ api.Companies
			),

			api.Companies
			] {
			case (companies, resources, location) =>
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
						addCompanyLinks (location, mkCompanyMap (companies))
						) (resources)
			}
			.mapK[F] (implicitly[ErrorOr ~> F])

	override protected lazy val factory =
		_.traverse {
			mkApiCompany.transform (_)
				.leftMap (ValidationError[domain.Company])
				.toEither
			}
			.flatMap {
				items =>
					api.Companies.from (companies = items)
			}


	private def addCompanyLinks (
		parent : ResourceLocation,
		knownCompanies : Map[String, domain.Company]
		)
		: api.Companies => ErrorOr[api.Companies] =
		companies.each.modifyA[ErrorOr] {
			resource =>
				for {
					company <- knownCompanies.get (resource.id.value)
						.toRight (
							LogicError (
								s"unable to resolve company for: ${resource.id.show}"
								)
							)

					subpath <- Path.from[ErrorOr, String] (
						s"/${company.id.toUuid ().toString}"
						)
					} yield CompanyToApi.defineLinksFor (
						tag[api.Company] (parent / subpath)
						) (company, resource)
			}


	private def mkCompanyMap (companies : Vector[domain.Company])
		: Map[String, domain.Company] =
		companies.map {
			company =>
				company.id.toUrn () -> company
			}
			.toMap
}


object CompaniesToApi
	extends AbstractToApi.Companion[domain.Company] ()
{
	/// Class Imports
	import refined.auto._


	/// Class Types
	final case class CreateCompany (override val href : URI)
		extends SemanticLink (
			method = Method.POST,
			href = href,
			rel = Relation (commandUrn ("create")),
			mediaType = MediaType.ApplicationJson,
			)


	private def mkLinks (location : ResourceLocation @@ api.Companies)
		: List[SemanticLink] =
		SemanticLink.Self (location.toUri ()) ::
		CreateCompany (location.toUri ()) ::
		Nil
}

