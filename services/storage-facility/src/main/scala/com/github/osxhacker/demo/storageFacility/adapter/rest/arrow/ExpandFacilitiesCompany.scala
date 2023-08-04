package com.github.osxhacker.demo.storageFacility.adapter.rest.arrow

import cats.ApplicativeThrow
import cats.data.{
	Kleisli,
	ValidatedNec
	}

import io.scalaland.chimney.TransformerF
import monocle.Focus

import com.github.osxhacker.demo.chassis.domain.error.DomainValueError
import com.github.osxhacker.demo.storageFacility.adapter.rest.api
import com.github.osxhacker.demo.storageFacility.domain


/**
 * The '''ExpandFacilitiesCompany''' type defines an
 * [[https://typelevel.org/cats/typeclasses/arrow.html Arrow]] capable of
 * associating the [[com.github.osxhacker.demo.storageFacility.domain.Company]]
 * which owns the
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]] instances
 * into the
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.api.StorageFacilities]]
 * as an `_embedded` member.
 *
 * The logic is essentially defined in terms of an
 * [[https://en.wikipedia.org/wiki/Endomorphism Endomorphism]] within a
 * [[cats.data.Kleisli]].  This way, multiple "expanders" can be chained
 * together easily, with the last one doing a `map (_._2)` in order to conform
 * with the
 * [[com.github.osxhacker.demo.chassis.adapter.rest.arrow.AbstractToApi]]
 * contract.
 */
final case class ExpandFacilitiesCompany[F[_]] ()
	(
		implicit
		private val applicative : ApplicativeThrow[F],
		private val chimney : TransformerF[
			ValidatedNec[String, +*],
			domain.Company,
			api.StorageFacilityCompanyView
			]
	)
{
	/// Class Imports
	import cats.syntax.option._
	import cats.syntax.validated._


	/// Class Types
	private type ArgumentsType = (
		Vector[domain.StorageFacility],
		api.StorageFacilities
		)


	/// Instance Properties
	private val companyEntry = Focus[api.Embedded] (_.values).at ("company")
	private val embed : api.StorageFacilities =>
		api.StorageFacilityCompanyView =>
		api.StorageFacilities =
		facilities => view => {
			api.StorageFacilities
				.Optics
				._embedded
				.modify {
					_.orElse (api.Embedded (Map.empty).some)
						.map (companyEntry.replace (view.some))
					} (facilities)
			}


	/**
	 * The apply method is an alias for `run`.
	 */
	def apply (owner : domain.Company)
		: Kleisli[F, ArgumentsType, ArgumentsType] =
		run (owner)


	/**
	 * The run method creates a [[cats.data.Kleisli]] which attempts to embed
	 * a [[com.github.osxhacker.demo.storageFacility.adapter.rest.api]]
	 * representation of a
	 * [[com.github.osxhacker.demo.storageFacility.domain.Company]].
	 */
	def run (owner : domain.Company)
		: Kleisli[F, ArgumentsType, ArgumentsType] =
		Kleisli {
			case (facilities, destination) =>
				chimney.transform (owner)
					.bimap (
						es => DomainValueError (
							es.toNonEmptyList
								.toList
								.mkString (",")
							),

						facilities -> embed (destination) (_)
						)
					.liftTo[F]
		}
}

