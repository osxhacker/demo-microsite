package com.github.osxhacker.demo.company.adapter.rest

import cats.arrow.Arrow
import cats.data.Kleisli
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

import com.github.osxhacker.demo.chassis.domain.NaturalTransformations
import com.github.osxhacker.demo.chassis.domain.event.EmitEvents
import com.github.osxhacker.demo.chassis.effect.{
	Pointcut,
	ReadersWriterResource
	}

import com.github.osxhacker.demo.company.adapter.RuntimeSettings
import com.github.osxhacker.demo.company.domain.{
	GlobalEnvironment,
	ScopedEnvironment
	}

import com.github.osxhacker.demo.company.domain.event.AllCompanyEvents
import com.github.osxhacker.demo.company.domain.scenario.{
	CreateCompany,
	LoadAllCompanies
	}


/**
 * The '''CompanyCollection''' type defines the [[sttp.tapir.Endpoint]]s
 * relating to providing the REST
 * [[com.github.osxhacker.demo.company.adapter.rest.api.Endpoints.Companies]]
 * collection resource.
 */
final case class CompanyCollection[F[_]] (
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
	import api.Endpoints.Companies.{
		GetCompaniesParams,
		PostCompaniesParams
		}

	import arrow._
	import cats.syntax.all._
	import log4cats.syntax._


	/// Class Types
	private object arrows
	{
		val companiesToApi = CompaniesToApi[F] ()
		val newCompanyFromApi = CompanyFromApi[api.NewCompany] ()
	}


	private object scenarios
		extends EmitEvents[ScopedEnvironment[F], AllCompanyEvents]
	{
		val create = CreateCompany[F] (api.NewCompany.Optics.slug) (
			arrows.newCompanyFromApi ()
			)

		val loadAll = LoadAllCompanies[F] ()
	}


	/// Instance Properties
	private[rest] val get : ServerEndpoint[Any, F] =
		api.Endpoints
			.Companies
			.getCompanies
			.publishUnderApi ()
			.extractPath ()
			.errorOut (statusCode)
			.serverLogicWithEnvironment[GlobalEnvironment[F]] {
				implicit global =>
					(loadAll _).tupled
				}

	private[rest] val post : ServerEndpoint[Any, F] =
		api.Endpoints
			.Companies
			.postCompanies
			.publishUnderApi ()
			.extractPath ()
			.out (ResourceLocation.asHeader)
			.out (statusCode (StatusCode.Created))
			.errorOut (statusCode)
			.serverLogicWithEnvironment[GlobalEnvironment[F]] {
				implicit global =>
					(create _).tupled
				}


	override def apply (): List[ServerEndpoint[Any, F]] =
		get ::
		post ::
		Nil


	private def create (
		params : PostCompaniesParams,
		company : api.NewCompany,
		path : Path
		)
		(implicit global : GlobalEnvironment[F])
		: F[ResultType[ResourceLocation]] =
		for {
			implicit0 (env : ScopedEnvironment[F]) <- global.scopeWith (
				params.`X-Correlation-ID`
				)
				.map (_.addContext (Map ("path" -> path.show)))

			implicit0 (log : StructuredLogger[F]) <- env.loggingFactory
				.create

			location <- ResourceLocation (path).pure[F]

			_ <- debug"creating company"
			created <- scenarios.create (company)

			_ <- debug"producing location for new company: ${created.id.show}"
			itemSubpath <- Path.from[F, String](s"/${created.id.toUuid ()}")

			result <- complete (location / itemSubpath)

			_ <- debug"finished creating company"
			} yield result


	private def loadAll (params : GetCompaniesParams, path : Path)
		(implicit global : GlobalEnvironment[F])
		: F[ResultType[api.Companies]] =
		for {
			implicit0 (env : ScopedEnvironment[F]) <- global.scopeWith (
				params.`X-Correlation-ID`
				)
				.map (_.addContext (Map ("path" -> path.show)))

			implicit0 (log : StructuredLogger[F]) <- env.loggingFactory
				.create

			_ <- debug"load all companies"
			existing <- scenarios.loadAll ().flatMap (_.compile.toVector)

			_ <- debug"loaded ${existing.size} companies"
			result <- completeF (
				arrows.companiesToApi ()
					.run (
						existing -> tag[api.Companies] (ResourceLocation (path))
						)
				)
		} yield result
}

