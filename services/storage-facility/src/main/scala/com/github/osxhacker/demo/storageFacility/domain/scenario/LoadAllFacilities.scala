package com.github.osxhacker.demo.storageFacility.domain.scenario

import cats.MonadThrow
import cats.data.Kleisli

import com.github.osxhacker.demo.chassis
import com.github.osxhacker.demo.chassis.effect.Pointcut
import com.github.osxhacker.demo.chassis.monitoring.metrics.UseCaseScenario
import com.github.osxhacker.demo.storageFacility.domain.{
	Company,
	ScopedEnvironment,
	StorageFacility
	}

import com.github.osxhacker.demo.storageFacility.domain.specification.FacilityBelongsTo


/**
 * The '''LoadAllFacilities''' type defines the Use-Case scenario responsible
 * for retrieving all
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]]
 * instances in the persistent store.
 *
 * Note that `factory` is defined in terms of the ''F'' container to allow for
 * arbitrary logic to produce a ''ResultT''.
 */
final case class LoadAllFacilities[F[_]] ()
	(
		implicit

		/// Needed for `pure`.
		private val monadThrow : MonadThrow[F],

		/// Needed for `measure`.
		private val pointcut : Pointcut[F]
	)
{
	/// Class Imports
	import cats.syntax.all._
	import chassis.syntax._


	override def toString () : String = "scenario: load all facilities"


	/**
	 * This version of the apply method supports retrieving all known
	 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]]
	 * instances without any transformation.
	 */
	def apply (tenant : Company)
		(implicit env : ScopedEnvironment[F])
		: F[fs2.Stream[F, StorageFacility]] =
		query (tenant).pure[F]
			.measure[
				UseCaseScenario[
					F,
					LoadAllFacilities[F],
					fs2.Stream[F, StorageFacility]
					]
				] ()


	/**
	 * This version of the apply method supports retrieving all known
	 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]]
	 * instances with __each__ transformed by the given '''factory'''.
	 */
	def apply[ResultT] (
		tenant : Company,
		factory : Kleisli[F, StorageFacility, ResultT]
		)
		(implicit env : ScopedEnvironment[F])
		: F[fs2.Stream[F, ResultT]] =
		query (tenant).evalMapChunk (factory.run)
			.pure[F]
			.measure[
				UseCaseScenario[
					F,
					LoadAllFacilities[F],
					fs2.Stream[F, ResultT]
					]
				] ()


	private def query (tenant : Company)
		(implicit env : ScopedEnvironment[F])
		: fs2.Stream[F, StorageFacility] =
		env.storageFacilities
			.queryBy (FacilityBelongsTo (tenant))
}

