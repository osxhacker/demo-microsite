package com.github.osxhacker.demo.company.adapter.rest.arrow

import cats.Endo
import cats.data.{
	Kleisli,
	ValidatedNec
	}

import io.scalaland.chimney.TransformerF
import com.github.osxhacker.demo.chassis.adapter.rest.arrow.AbstractFromApi
import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.company.adapter.rest.api
import com.github.osxhacker.demo.company.domain.Company


/**
 * The '''CompanyFromApi''' type defines the
 * [[com.github.osxhacker.demo.chassis.adapter.rest.arrow.AbstractFromApi]]
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
final case class CompanyFromApi[ApiT] ()
	(
		implicit
		private val chimney : TransformerF[
			ValidatedNec[String, +*],
			ApiT,
			Company
			],

		private val clear : CompanyFromApi.ClearProperties[ApiT]
	)
	extends AbstractFromApi[
		Kleisli[ErrorOr, *, *],
		ValidatedNec[String, +*],
		ApiT,
		Company
		] ()
{
	/// Instance Properties
	override protected val prepare = clear ()
	override protected val factory = chimney.transform
}


object CompanyFromApi
{
	/// Class Types
	/**
	 * The '''ClearProperties''' type is a model of the TYPE CLASS pattern and
	 * defines the contract for ensuring properties to be ignored in ''ApiT''
	 * are removed.
	 */
	sealed trait ClearProperties[ApiT]
	{
		def apply () : Endo[ApiT]
	}


	object ClearProperties
	{
		/// Implicit Conversions
		implicit val newCompany =
			new ClearProperties[api.NewCompany] {
				override def apply () : Endo[api.NewCompany] = identity
				}

		implicit val company =
			new ClearProperties[api.Company] {
				/// Class Imports
				import api.Company.Optics


				override def apply () : Endo[api.Company] =
					Optics._links
						.replace (None)
						.andThen (Optics._embedded.replace (None) (_))
				}
	}
}

