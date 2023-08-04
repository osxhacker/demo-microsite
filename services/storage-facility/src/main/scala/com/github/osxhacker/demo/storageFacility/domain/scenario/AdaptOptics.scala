package com.github.osxhacker.demo.storageFacility.domain.scenario

import cats.data.{
	Kleisli,
	ValidatedNec
	}

import eu.timepit.refined
import eu.timepit.refined.api.{
	Refined,
	RefinedType
	}

import monocle.Getter
import squants.space.{
	CubicMeters,
	Volume
	}

import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.chassis.domain.entity.{
	Identifier,
	Version
	}

import com.github.osxhacker.demo.storageFacility.domain.{
	StorageFacility,
	StorageFacilityStatus
	}


/**
 * The '''AdaptOptics''' `object` defines the algorithms for defining
 * [[cats.data.Kleisli]]s based on [[monocle]] optics types for use in
 * interacting with ''SourceT'' instances to provide properties relevant to
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]].
 */
private[scenario] object AdaptOptics
{
	/// Class Imports
	import cats.syntax.either._
	import cats.syntax.validated._
	import mouse.any._
	import refined.cats.syntax._
	import refined.numeric.NonNegative


	/// Class Types
	type F[+A] = ValidatedNec[String, A]
	type KleisliType[SourceT, A] = Kleisli[F, SourceT, A]


	@inline
	def apply[SourceT, ValueT] (lens : Getter[SourceT, ValueT])
		: KleisliType[SourceT, ValueT] =
		Kleisli[F, SourceT, ValueT] {
			lens.get (_)
				.validNec
			}


	@inline
	def availability[SourceT, T, P] (lens : Getter[SourceT, Refined[T, P]])
		(implicit numeric : Numeric[T])
		: KleisliType[SourceT, Volume] =
		volume (lens)


	@inline
	def capacity[SourceT, T, P] (lens : Getter[SourceT, Refined[T, P]])
		(implicit numeric : Numeric[T])
		: KleisliType[SourceT, Volume] =
		volume (lens)


	@inline
	def id[SourceT, CandidateT] (lens : Getter[SourceT, CandidateT])
		(implicit parser : Identifier.Parser[StorageFacility, CandidateT])
		: KleisliType[SourceT, Identifier[StorageFacility]] =
		Kleisli[F, SourceT, Identifier[StorageFacility]] {
			candidate =>
				parser (lens.get (candidate))
					.toEither
					.leftMap (_.getMessage)
					.toValidatedNec
			}


	@inline
	def name[SourceT, P] (lens : Getter[SourceT, Refined[String, P]])
		: KleisliType[SourceT, StorageFacility.Name] =
		Kleisli[F, SourceT, StorageFacility.Name] {
			candidate =>
				lens.get (candidate).value |> StorageFacility.Name.validateNec
			}


	@inline
	def status[SourceT, StatusT <: AnyRef] (lens : Getter[SourceT, StatusT])
		: KleisliType[SourceT, StorageFacilityStatus] =
		Kleisli[F, SourceT, StorageFacilityStatus] {
			candidate =>
				StorageFacilityStatus.withNameInsensitiveEither (
					lens.get (candidate).toString
					)
					.leftMap (_.getMessage)
					.toValidatedNec
			}


	@inline
	def version[SourceT, P] (lens : Getter[SourceT, Refined[Int, P]])
		: KleisliType[SourceT, Version] =
		Kleisli {
			source =>
				Version[ErrorOr] (lens.get (source).value)
					.leftMap (_.getMessage)
					.toValidatedNec
			}


	private def volume[SourceT, T, P] (lens : Getter[SourceT, Refined[T, P]])
		(implicit numeric : Numeric[T])
		: KleisliType[SourceT, Volume] =
		Kleisli (
			lens.andThen (
				Getter[Refined[T, P], F[Volume]] (
					r =>
						RefinedType[Refined[T, NonNegative]].refine (r.value)
							.map (pos => CubicMeters (pos.value))
							.toValidatedNec
					)
				)
				.get (_)
			)
}

