package com.github.osxhacker.demo.storageFacility.adapter.rest

import cats.arrow.Arrow
import cats.data.Kleisli
import io.scalaland.chimney
import org.typelevel.log4cats
import org.typelevel.log4cats.{
	LoggerFactory,
	StructuredLogger
	}

import shapeless.tag
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

import com.github.osxhacker.demo.chassis.domain.event.EmitEvents
import com.github.osxhacker.demo.chassis.effect.{
	Pointcut,
	ReadersWriterResource
	}

import com.github.osxhacker.demo.storageFacility.adapter.RuntimeSettings
import com.github.osxhacker.demo.storageFacility.domain
import com.github.osxhacker.demo.storageFacility.domain.{
	CompanyReference,
	GlobalEnvironment,
	ScopedEnvironment
	}

import com.github.osxhacker.demo.storageFacility.domain.event.AllStorageFacilityEvents
import com.github.osxhacker.demo.storageFacility.domain.scenario._


/**
 * The '''StorageFacilityCollection''' type defines the [[sttp.tapir.Endpoint]]s
 * relating to providing the REST
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.api.Endpoints.StorageFacilities]]
 * collection resource.
 */
final case class StorageFacilityCollection[F[_]] (
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
	import api.Endpoints.StorageFacilities.{
		GetStorageFacilitiesParams,
		PutStorageFacilitiesParams
		}

	import arrow._
	import cats.syntax.all._
	import chimney.cats._
	import chimney.dsl._
	import domain.transformers._
	import log4cats.syntax._


	/// Class Types
	private object arrows
	{
		val expandCompany = ExpandFacilitiesCompany[F] ()
		val facilitiesToApi = FacilitiesToApi[F] ()
		val newFacilityFromApi = FacilityFromApi[api.NewStorageFacility] ()
	}


	private object scenarios
		extends EmitEvents[ScopedEnvironment[F], AllStorageFacilityEvents]
	{
		val create = AddNewFacility[F] (
			api.NewStorageFacility.Optics.available,
			api.NewStorageFacility.Optics.capacity
			) (arrows.newFacilityFromApi ())

		val findCompany = FindActiveCompany[F] ()
		val loadAll = LoadAllFacilities[F] ()
	}


	/// Instance Properties
	private[rest] val get : ServerEndpoint[Any, F] =
		api.Endpoints
			.StorageFacilities
			.getStorageFacilities
			.publishUnderApi ()
			.extractPath ()
			.errorOut (statusCode)
			.serverLogicWithEnvironment[GlobalEnvironment[F]] {
				implicit global =>
					(loadAll _).tupled
				}

	private[rest] val put : ServerEndpoint[Any, F] =
		api.Endpoints
			.StorageFacilities
			.putStorageFacilities
			.publishUnderApi ()
			.extractPath ()
			.out (ResourceLocation.asHeader)
			.out (statusCode (StatusCode.Created))
			.errorOut (statusCode)
			.serverLogicWithEnvironment[GlobalEnvironment[F]] {
				implicit global =>
					(create _).tupled
				}


	override def apply () : List[ServerEndpoint[Any, F]] =
		get ::
		put ::
		Nil


	private def create (
		params : PutStorageFacilitiesParams,
		facility : api.NewStorageFacility,
		path : Path
		)
		(implicit global : GlobalEnvironment[F])
		: F[ResultType[ResourceLocation]] =
		for {
			slug <- Slug[F] (params.company)

			implicit0 (env : ScopedEnvironment[F]) <- global.scopeWith (
				CompanyReference (slug),
				params.`X-Correlation-ID`
				)
				.map (_.addContext (Map ("path" -> path.show)))

			implicit0 (log : StructuredLogger[F]) <- env.loggingFactory
				.create

			location <- ResourceLocation (path).pure[F]

			_ <- debug"creating storage facility"
			created <- scenarios.create (facility)

			_ <- debug"producing location for new facility: ${created.id.show}"
			itemSubpath <- Path.from[F, String] (s"/${created.id.toUuid ()}")
			result <- complete (location / itemSubpath)

			_ <- debug"finished creating storage facility"
			} yield result


	private def loadAll (params : GetStorageFacilitiesParams, path : Path)
		(implicit global : GlobalEnvironment[F])
		: F[ResultType[api.StorageFacilities]] =
		for {
			slug <- Slug[F] (params.company)

			implicit0 (env : ScopedEnvironment[F]) <- global.scopeWith (
				CompanyReference (slug),
				params.`X-Correlation-ID`
				)
				.map (_.addContext (Map ("path" -> path.show)))

			implicit0 (log : StructuredLogger[F]) <- env.loggingFactory
				.create

			_ <- debug"resolve owning company"
			owner <- scenarios.findCompany (env.tenant)

			_ <- debug"load all storage facilities"
			existing <- scenarios.loadAll (owner).flatMap (_.compile.toVector)

			_ <- debug"loaded ${existing.size} storage facilities"
			result <- completeF (
				arrows.facilitiesToApi (embed (owner, params.expand))
					.run (
						existing -> tag[api.StorageFacilities] (
							ResourceLocation (path)
							)
						)
				)
			} yield result


	private def embed (
		owner : domain.Company,
		requested : Option[api.StorageFacilitiesExpansion]
		)
		: Kleisli[
			F,
			(Vector[domain.StorageFacility], api.StorageFacilities),
			api.StorageFacilities
			] =
		requested match {
			case Some (
				api.StorageFacilitiesExpansion.all |
				api.StorageFacilitiesExpansion.company
				) =>
				arrows.expandCompany (owner).map (_._2)

			case _ =>
				Kleisli (_._2.pure)
			}
}

