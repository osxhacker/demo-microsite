package com.github.osxhacker.demo.storageFacility.adapter.rest

import cats.arrow.Arrow
import cats.data.Kleisli
import io.scalaland.chimney
import monocle.macros.GenLens
import org.typelevel.log4cats
import org.typelevel.log4cats.{
	LoggerFactory,
	StructuredLogger
	}

import shapeless.{
	Witness,
	tag
	}

import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.server.ServerEndpoint

import com.github.osxhacker.demo.chassis.adapter.rest.{
	Path,
	ResourceLocation
	}

import com.github.osxhacker.demo.chassis.domain.{
	NaturalTransformations,
	Slug
	}

import com.github.osxhacker.demo.chassis.domain.algorithm.FindField
import com.github.osxhacker.demo.chassis.domain.event.EmitEvents
import com.github.osxhacker.demo.chassis.effect.{
	Pointcut,
	ReadersWriterResource
	}

import com.github.osxhacker.demo.storageFacility.adapter.RuntimeSettings
import com.github.osxhacker.demo.storageFacility.adapter.rest.api.Endpoints
import com.github.osxhacker.demo.storageFacility.domain
import com.github.osxhacker.demo.storageFacility.domain.{
	CompanyReference,
	GlobalEnvironment,
	ScopedEnvironment,
	StorageFacilityStatus
	}

import com.github.osxhacker.demo.storageFacility.domain.event.AllStorageFacilityEvents
import com.github.osxhacker.demo.storageFacility.domain.scenario._


/**
 * The '''StorageFacilityResource''' type defines the [[sttp.tapir.Endpoint]]s
 * relating to providing the REST
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.api.Endpoints.StorageFacilities]]
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.api.StorageFacility]]
 * individual resources.
 */
final case class StorageFacilityResource[F[_]] (
	override protected val settings : RuntimeSettings
	)
	(
		implicit

		/// Needed for `serverLogicWithEnvironment`.
		override protected val environment : ReadersWriterResource[
			F,
			GlobalEnvironment[F]
			],

		/// Needed for API arrows.
		private val kleisliArrow : Arrow[Kleisli[F, *, *]],

		/// Needed for `log4cats.syntax`.
		override protected val loggerFactory : LoggerFactory[F],

		/// Needed for `serverLogicWithEnvironment`.
		private val pointcut : Pointcut[F],

		/// Needed for `complete`, `compile`, `failWith`, and 'flatMap'.
		private val compiler : fs2.Compiler.Target[F]
	)
	extends AbstractResource[F] ()
		with NaturalTransformations
{
	/// Class Imports
	import Endpoints.StorageFacilities._
	import arrow._
	import cats.syntax.all._
	import chimney.cats._
	import chimney.dsl._
	import domain.transformers._
	import mouse.any._
	import mouse.boolean._
	import log4cats.syntax._


	/// Class Types
	private object arrows
	{
		val expandCompany = ExpandFacilityCompany[F] ()
		val fromApi = FacilityFromApi[api.StorageFacility] ()
		val toApi = FacilityToApi[F] ()
	}


	private object scenarios
		extends EmitEvents[ScopedEnvironment[F], AllStorageFacilityEvents]
	{
		/// Class Imports
		import api.StorageFacility.{
			Optics => FacilityOptics
			}

		val change = ChangeFacility[F] (
			id = FacilityOptics.id,
			version = FacilityOptics.version,
			status = FacilityOptics.status,
			available = FacilityOptics.available,
			capacity = FacilityOptics.capacity
			) (arrows.fromApi ())

		val changeStatus = ChangeFacilityStatus[F] (
			api.VersionOnly
				.Optics
				.version
			)

		val findForActivate = LoadFacility[F] (
			GenLens[PostStorageFacilitiesAndFacilityActivateParams] (_.facility)
			)

		val findForChange = LoadFacility[F] (
			GenLens[PostStorageFacilitiesAndFacilityParams] (_.facility)
			)

		val findForClose = LoadFacility[F] (
			GenLens[PostStorageFacilitiesAndFacilityCloseParams] (_.facility)
			)

		val findForRemove = LoadFacility[F] (
			GenLens[DeleteStorageFacilitiesAndFacilityParams] (_.facility)
			)

		val findForRetrieve = LoadFacility[F] (
			GenLens[GetStorageFacilitiesAndFacilityParams] (_.facility)
			)

		val remove = DeleteFacility[F] ()
	}


	/// Instance Properties
	private[rest] val activate : ServerEndpoint[Any, F] =
		api.Endpoints
			.StorageFacilities
			.postStorageFacilitiesAndFacilityActivate
			.publishUnderApi ()
			.extractPath ()
			.errorOut (statusCode)
			.serverLogicWithEnvironment[GlobalEnvironment[F]] {
				implicit global =>
					(
						changeStatus (
							scenarios.findForActivate,
							StorageFacilityStatus.Active
							) _
					).tupled
				}

	private[rest] val close : ServerEndpoint[Any, F] =
		api.Endpoints
			.StorageFacilities
			.postStorageFacilitiesAndFacilityClose
			.publishUnderApi ()
			.extractPath ()
			.errorOut (statusCode)
			.serverLogicWithEnvironment[GlobalEnvironment[F]] {
				implicit global =>
					(
						changeStatus (
							scenarios.findForClose,
							StorageFacilityStatus.Closed
							) _
					)
					.tupled
				}

	private[rest] val delete : ServerEndpoint[Any, F] =
		api.Endpoints
			.StorageFacilities
			.deleteStorageFacilitiesAndFacility
			.publishUnderApi ()
			.extractPath ()
			.errorOut (statusCode)
			.serverLogicWithEnvironment[GlobalEnvironment[F]] {
				implicit global =>
					(remove _).tupled
				}

	private[rest] val get : ServerEndpoint[Any, F] =
		api.Endpoints
			.StorageFacilities
			.getStorageFacilitiesAndFacility
			.publishUnderApi ()
			.extractPath ()
			.errorOut (statusCode)
			.serverLogicWithEnvironment[GlobalEnvironment[F]] {
				implicit global =>
					(retrieve _).tupled
				}

	private[rest] val post : ServerEndpoint[Any, F] =
		api.Endpoints
			.StorageFacilities
			.postStorageFacilitiesAndFacility
			.publishUnderApi ()
			.extractPath ()
			.errorOut (statusCode)
			.serverLogicWithEnvironment[GlobalEnvironment[F]] {
				implicit global =>
					(change _).tupled
				}


	override def apply () =
		activate ::
		close ::
		delete ::
		get ::
		post ::
		Nil


	private def change (
		params : PostStorageFacilitiesAndFacilityParams,
		desired : api.StorageFacility,
		path : Path
		)
		(implicit global : GlobalEnvironment[F])
		: F[ResultType[api.StorageFacility]] =
		for {
			slug <- Slug[F] (params.company)

			implicit0 (env : ScopedEnvironment[F]) <- createScopedEnvironment (
				CompanyReference (slug),
				path,
				params
				)

			implicit0 (log : StructuredLogger[F]) <- env.loggingFactory
				.create

			_ <- debug"looking up storage facility: ${params.facility}"
			existing <- scenarios.findForChange (params)

			_ <- debug"changing existing storage facility: ${existing.id.show} ${existing.version.show}"
			altered <- scenarios.change (existing, desired)

			_ <- debug"saved storage facility: ${altered.id.show} ${altered.version.show}"
			result <- completeF (
				arrows.toApi (embed (params.expand))
					.run (
						 altered -> tag[api.StorageFacility] (
							  ResourceLocation (path)
							  )
						 )
				)
			} yield result


	private def changeStatus[ParamsT <: AnyRef] (
		find : LoadFacility[F, ParamsT, String],
		desired : StorageFacilityStatus
		)
		(
			params : ParamsT,
			body : api.VersionOnly,
			path : Path
		)
		(
			implicit
			global : GlobalEnvironment[F],
			correlationId : FindField[
				ParamsT,
				Witness.`'X-Correlation-ID`.T,
				String
				],

			companyId : FindField[ParamsT, Witness.`'company`.T, String],
			expand : FindField[
				ParamsT,
				Witness.`'expand`.T,
				Option[api.StorageFacilityExpansion]
				],

			facility : FindField[ParamsT, Witness.`'facility`.T, String]
		)
		: F[ResultType[api.StorageFacility]] =
		for {
			slug <- Slug[F] (companyId (params))

			implicit0 (env : ScopedEnvironment[F]) <- createScopedEnvironment (
				CompanyReference (slug),
				path,
				params
				)

			implicit0 (log : StructuredLogger[F]) <- env.loggingFactory
				.create

			_ <- debug"looking up storage facility: ${facility (params)}"
			existing <- find (params)

			_ <- debug"changing existing storage facility: ${existing.id.show} ${existing.version.show}"
			altered <- scenarios.changeStatus (existing, body, desired)

			_ <- debug"saved storage facility: ${altered.id.show} ${altered.version.show}"
			result <- completeF (
				arrows.toApi (expand (params) |> embed)
					.run (
						altered -> tag[api.StorageFacility] (
							ResourceLocation (path.parent)
							)
						)
				)
			} yield result


	private def remove (
		params : DeleteStorageFacilitiesAndFacilityParams,
		path : Path
		)
		(implicit global : GlobalEnvironment[F])
		: F[ResultType[Unit]] =
		for {
			slug <- Slug[F] (params.company)

			implicit0 (env : ScopedEnvironment[F]) <- createScopedEnvironment (
				CompanyReference (slug),
				path,
				params
				)

			implicit0 (log : StructuredLogger[F]) <- env.loggingFactory
				.create

			_ <- debug"loading ${params.facility} storage facility"
			facility <- scenarios.findForRemove (params)

			_ <- debug"removing ${params.facility} storage facility"
			deleted <- scenarios.remove (facility)

			_ <- debug"removed ${params.facility}? $deleted"
			result <- deleted.fold (
				complete (),
				failWith (
					api.ObjectNotFoundDetails.from (
						id = params.facility.merge.toString,
						status = StatusCode.Gone.code,
						title = "storage facility could not be removed"
						)
					)
				)
			} yield result


	private def retrieve (
		params : GetStorageFacilitiesAndFacilityParams,
		path : Path
		)
		(implicit global : GlobalEnvironment[F])
		: F[ResultType[api.StorageFacility]] =
		for {
			slug <- Slug[F] (params.company)

			implicit0 (env : ScopedEnvironment[F]) <- createScopedEnvironment (
				CompanyReference (slug),
				path,
				params
				)

			implicit0 (log : StructuredLogger[F]) <- env.loggingFactory
				.create

			_ <- debug"loading ${params.facility} storage facility"
			facility <- scenarios.findForRetrieve (params)
			result <- completeF (
				arrows.toApi (embed (params.expand)).run (
					facility -> tag[api.StorageFacility] (ResourceLocation (path))
					)
				)
			} yield result


	private def embed (requested : Option[api.StorageFacilityExpansion])
		: Kleisli[
			F,
			(domain.StorageFacility, api.StorageFacility),
			api.StorageFacility
			] =
		requested match {
			case Some (
				api.StorageFacilityExpansion.all |
				api.StorageFacilityExpansion.company
				) =>
				arrows.expandCompany ().map (_._2)

			case _ =>
				Kleisli (_._2.pure)
		}
}

