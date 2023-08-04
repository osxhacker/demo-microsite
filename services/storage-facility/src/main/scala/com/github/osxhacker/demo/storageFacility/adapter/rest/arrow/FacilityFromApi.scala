package com.github.osxhacker.demo.storageFacility.adapter.rest.arrow

import cats.Endo
import cats.data.Kleisli
import io.scalaland.chimney
import monocle.macros.GenLens
import shapeless.{
	syntax => _,
	_
	}

import com.github.osxhacker.demo.chassis.adapter.rest.arrow.AbstractFromApi
import com.github.osxhacker.demo.chassis.domain.{
	ChimneyErrors,
	ErrorOr
	}

import com.github.osxhacker.demo.chassis.domain.entity.{
	Identifier,
	ModificationTimes,
	Version
	}

import com.github.osxhacker.demo.chassis.domain.event.Region
import com.github.osxhacker.demo.storageFacility.adapter.rest.api
import com.github.osxhacker.demo.storageFacility.domain
import com.github.osxhacker.demo.storageFacility.domain.{
	Company,
	StorageFacility
	}


/**
 * The '''FacilityFromApi''' type defines the
 * [[com.github.osxhacker.demo.chassis.adapter.rest.arrow.AbstractFromApi]]
 * arrow which attempts to produce a
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]]
 * from an
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.api]] type and
 * requisite [[com.github.osxhacker.demo.storageFacility.domain]] collaborators.
 */
final case class FacilityFromApi[ApiT] ()
	(
		implicit
		private val clear : FacilityFromApi.ClearProperties[ApiT],
		private val create : FacilityFromApi.CreateInstance[ApiT]
	)
	extends AbstractFromApi[
		Kleisli[ErrorOr, *, *],
		ChimneyErrors,
		FacilityFromApi.InputType[ApiT],
		StorageFacility
		] ()
{
	/// Instance Properties
	override protected val prepare = clear ()
	override protected val factory = create (_)
}


object FacilityFromApi
{
	/// Class Imports
	import shapeless.nat._


	/// Class Types
	/**
	 * The '''InputType''' `type` defines the contract for what is needed in
	 * order to invoke the '''FacilityFromApi''' arrow.
	 */
	type InputType[ApiT] = ApiT :: Region :: Company :: HNil


	/**
	 * The '''ClearProperties''' type is a model of the TYPE CLASS pattern and
	 * defines the contract for ensuring properties to be ignored in ''ApiT''
	 * are removed.
	 */
	sealed trait ClearProperties[ApiT]
	{
		def apply () : Endo[InputType[ApiT]]
	}


	object ClearProperties
	{
		/// Implicit Conversions
		implicit val newStorageFacility =
			new ClearProperties[api.NewStorageFacility] {
				override def apply ()
					: Endo[InputType[api.NewStorageFacility]] =
					identity
				}

		implicit val storageFacility =
			new ClearProperties[api.StorageFacility] {
				/// Class Imports
				import api.StorageFacility.Optics


				override def apply () : Endo[InputType[api.StorageFacility]] =
				{
					val pair = GenLens[InputType[api.StorageFacility]] (_.head)

					pair.andThen (Optics._links)
						.replace (None)
						.andThen (
							pair.andThen (Optics._embedded)
								.replace (None) (_)
							)
				}
			}
	}


	sealed trait CreateInstance[ApiT]
	{
		def apply (source : InputType[ApiT]) : ChimneyErrors[StorageFacility]
	}


	object CreateInstance
	{
		/// Class Imports
		import cats.syntax.option._
		import chimney.cats._
		import chimney.dsl._
		import domain.transformers._


		/// Implicit Conversions
		implicit val newStorageFacility
			: CreateInstance[api.NewStorageFacility] =
			new CreateInstance[api.NewStorageFacility] {
				override def apply (input : InputType[api.NewStorageFacility])
					: ChimneyErrors[StorageFacility] =
				{
					val source :: region :: owner :: HNil = input

					source.intoF[ChimneyErrors, domain.StorageFacility]
						.withFieldComputed (
							_.id,
							_ => Identifier.fromRandom[domain.StorageFacility] ()
							)
						.withFieldConst (_.version, Version.initial)
						.withFieldConst (_.owner, owner)
						.withFieldConst (_.primary, region.some)
						.withFieldComputed (
							_.timestamps,
							_ => ModificationTimes.now ()
							)
						.transform
				}
			}


		implicit val storageFacility : CreateInstance[api.StorageFacility] =
			new CreateInstance[api.StorageFacility] {
				override def apply (input : InputType[api.StorageFacility])
					: ChimneyErrors[StorageFacility] =
				{
					val source :: region :: owner :: HNil = input

					source.intoF[ChimneyErrors, domain.StorageFacility]
						.withFieldConst (_.owner, owner)
						.withFieldConst (_.primary, region.some)
						.withFieldComputed (
							_.timestamps,
							rec =>
								ModificationTimes (
									rec.createdOn.toInstant,
									rec.lastChanged.toInstant
								)
							)
						.transform
				}
			}
	}
}

