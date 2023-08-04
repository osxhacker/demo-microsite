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
 * The '''ExpandFacilityCompany''' type defines an
 * [[https://typelevel.org/cats/typeclasses/arrow.html Arrow]] capable of
 * associating the [[com.github.osxhacker.demo.storageFacility.domain.Company]]
 * which owns a
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]] into the
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.api.StorageFacility]]
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
final case class ExpandFacilityCompany[F[_]] ()
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
	private type ArgumentsType = (domain.StorageFacility, api.StorageFacility)


	/// Instance Properties
	private val companyEntry = Focus[api.Embedded] (_.values).at ("company")
	private val embed : api.StorageFacility =>
		api.StorageFacilityCompanyView =>
		api.StorageFacility =
		facility => view => {
			api.StorageFacility
				.Optics
				._embedded
				.modify {
					_.orElse (api.Embedded (Map.empty).some)
						.map (companyEntry.replace (view.some))
					} (facility)
			}


	/**
	 * The apply method is an alias for `run`.
	 */
	def apply () : Kleisli[F, ArgumentsType, ArgumentsType] = run ()


	/**
	 * The run method creates a [[cats.data.Kleisli]] which attempts to embed
	 * a [[com.github.osxhacker.demo.storageFacility.adapter.rest.api]]
	 * representation of a
	 * [[com.github.osxhacker.demo.storageFacility.domain.Company]].
	 */
	def run () : Kleisli[F, ArgumentsType, ArgumentsType] =
		Kleisli {
			case (source, destination) =>
				chimney.transform (source.owner)
					.bimap (
						es => DomainValueError (
							es.toNonEmptyList.toList.mkString (",")
							),

						source -> embed (destination) (_)
						)
					.liftTo[F]
		}
}

