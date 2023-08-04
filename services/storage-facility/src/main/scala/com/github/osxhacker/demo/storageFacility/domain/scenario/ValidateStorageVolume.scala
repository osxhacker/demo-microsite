package com.github.osxhacker.demo.storageFacility.domain.scenario

import cats.data._
import monocle.Getter
import squants.space.{
	CubicMeters,
	Volume
	}


/**
 * The '''ValidateStorageVolume''' `object` defines the algorithm for validating
 * the [[squants.space.Volume]]s associated with a
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]].
 */
private[scenario] object ValidateStorageVolume
{
	/// Class Imports
	import cats.syntax.apply._
	import cats.syntax.validated._


	/// Class Types
	type F[+A] = ValidatedNec[String, A]


	/**
	 * When validation succeeds, the '''CapacityAndAvailable''' contains the
	 * `available` and `capacity` [[squants.space.Volume]]s.
	 */
	final case class CapacityAndAvailable (
		val available : Volume,
		val capacity : Volume
		)


	/// Instance Properties
	private val capacityHoldsAvailable =
		NonEmptyChain.one ("available space must not exceed capacity")

	private val negativeVolume =
		NonEmptyChain.one ("negative volumes are not allowed")

	private val zero = CubicMeters (0)


	def apply[SourceT] (
		source : SourceT,
		available : Getter[SourceT, Volume],
		capacity : Getter[SourceT, Volume]
		)
		: F[CapacityAndAvailable] =
		apply[SourceT] (
			source,
			Kleisli[F, SourceT, Volume] (available.get (_).validNec),
			Kleisli[F, SourceT, Volume] (capacity.get (_).validNec)
			)


	def apply[SourceT] (
		source : SourceT,
		available : Kleisli[F, SourceT, Volume],
		capacity : Kleisli[F, SourceT, Volume]
		)
		: F[CapacityAndAvailable] =
		(
			checkAvailable (source, available),
			checkCapacity (source, capacity),
		).mapN (CapacityAndAvailable)
			.ensure (capacityHoldsAvailable) {
				caa =>
					caa.capacity >= caa.available
				}


	private def checkAvailable[SourceT] (
		source : SourceT,
		available : Kleisli[F, SourceT, Volume]
		)
		: F[Volume] =
		available (source) andThen validate


	private def checkCapacity[SourceT] (
		source : SourceT,
		capacity : Kleisli[F, SourceT, Volume]
		)
		: F[Volume] =
		capacity (source) andThen validate


	private def validate (candidate : Volume) : F[Volume] =
		Validated.cond (candidate >= zero, candidate, negativeVolume)
}

