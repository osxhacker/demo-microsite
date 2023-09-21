package com.github.osxhacker.demo.company.adapter.rest

import cats.arrow.Arrow
import cats.data.Kleisli
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

import com.github.osxhacker.demo.chassis.domain.NaturalTransformations
import com.github.osxhacker.demo.chassis.domain.algorithm.FindField
import com.github.osxhacker.demo.chassis.domain.event.EmitEvents
import com.github.osxhacker.demo.chassis.effect.{
	Pointcut,
	ReadersWriterResource
	}

import com.github.osxhacker.demo.company.adapter.RuntimeSettings
import com.github.osxhacker.demo.company.adapter.rest.api.Endpoints
import com.github.osxhacker.demo.company.domain.{
	CompanyStatus,
	GlobalEnvironment,
	ScopedEnvironment
	}

import com.github.osxhacker.demo.company.domain.event.AllCompanyEvents
import com.github.osxhacker.demo.company.domain.scenario._


/**
 * The '''CompanyResource''' type defines the [[sttp.tapir.Endpoint]]s relating
 * to providing the REST
 * [[com.github.osxhacker.demo.company.adapter.rest.api.Endpoints.Companies]]
 * [[com.github.osxhacker.demo.company.adapter.rest.api.Company]] individual
 * resources.
 */
final case class CompanyResource[F[_]] (
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
	import Endpoints.Companies.{
		DeleteCompaniesByCompanyParams,
		GetCompaniesByCompanyParams,
		PostCompaniesByCompanyParams,
		PostCompaniesByCompanyActivateParams,
		PostCompaniesByCompanyDeactivateParams,
		PostCompaniesByCompanySuspendParams
		}

	import arrow._
	import cats.syntax.all._
	import mouse.boolean._
	import log4cats.syntax._


	/// Class Types
	private object arrows
	{
		val fromApi = CompanyFromApi[api.Company] ()
		val toApi = CompanyToApi[F] ()
	}


	private object scenarios
		extends EmitEvents[ScopedEnvironment[F], AllCompanyEvents]
	{
		/// Class Imports
		import api.Company.{
			Optics => CompanyOptics
			}


		val change = ChangeCompany[F] (
			CompanyOptics.id,
			CompanyOptics.version,
			CompanyOptics.slug,
			CompanyOptics.status
			) (arrows.fromApi ())

		val changeStatus = ChangeCompanyStatus[F] (
			api.VersionOnly
				.Optics
				.version
			)

		val findForActivate = FindCompany[F] (
			GenLens[PostCompaniesByCompanyActivateParams] (_.company)
			)

		val findForChange = FindCompany[F] (
			GenLens[PostCompaniesByCompanyParams] (_.company)
			)

		val findForDeactivate = FindCompany[F] (
			GenLens[PostCompaniesByCompanyDeactivateParams] (_.company)
			)

		val findForRemove = FindCompany[F] (
			GenLens[DeleteCompaniesByCompanyParams] (_.company)
			)

		val findForRetrieve = FindCompany[F] (
			GenLens[GetCompaniesByCompanyParams] (_.company)
			)

		val findForSuspend = FindCompany[F] (
			GenLens[PostCompaniesByCompanySuspendParams] (_.company)
			)

		val remove = DeleteCompany[F] ()
	}


	/// Instance Properties
	private[rest] val activate : ServerEndpoint[Any, F] =
		api.Endpoints
			.Companies
			.postCompaniesByCompanyActivate
			.publishUnderApi ()
			.extractPath ()
			.errorOut (statusCode)
			.serverLogicWithEnvironment[GlobalEnvironment[F]] {
				implicit global =>
					(
						changeStatus (
							scenarios.findForActivate,
							CompanyStatus.Active
							) _
					).tupled
				}

	private[rest] val deactivate : ServerEndpoint[Any, F] =
		api.Endpoints
			.Companies
			.postCompaniesByCompanyDeactivate
			.publishUnderApi ()
			.extractPath ()
			.errorOut (statusCode)
			.serverLogicWithEnvironment[GlobalEnvironment[F]] {
				implicit global =>
					(
						changeStatus (
							scenarios.findForDeactivate,
							CompanyStatus.Inactive
							) _
					).tupled
				}

	private[rest] val delete : ServerEndpoint[Any, F] =
		api.Endpoints
			.Companies
			.deleteCompaniesByCompany
			.publishUnderApi ()
			.extractPath ()
			.errorOut (statusCode)
			.serverLogicWithEnvironment[GlobalEnvironment[F]] {
				implicit global =>
					(remove _).tupled
				}

	private[rest] val get : ServerEndpoint[Any, F] =
		api.Endpoints
			.Companies
			.getCompaniesByCompany
			.publishUnderApi ()
			.extractPath ()
			.errorOut (statusCode)
			.serverLogicWithEnvironment[GlobalEnvironment[F]] {
				implicit global =>
					(retrieve _).tupled
				}

	private[rest] val post : ServerEndpoint[Any, F] =
		api.Endpoints
			.Companies
			.postCompaniesByCompany
			.publishUnderApi ()
			.extractPath ()
			.errorOut (statusCode)
			.serverLogicWithEnvironment[GlobalEnvironment[F]] {
				implicit global =>
					(change _).tupled
				}

	private[rest] val suspend : ServerEndpoint[Any, F] =
		api.Endpoints
			.Companies
			.postCompaniesByCompanySuspend
			.publishUnderApi ()
			.extractPath ()
			.errorOut (statusCode)
			.serverLogicWithEnvironment[GlobalEnvironment[F]] {
				implicit global =>
					(
						changeStatus (
							scenarios.findForSuspend,
							CompanyStatus.Suspended
							) _
					).tupled
				}


	override def apply () =
		activate ::
		deactivate ::
		suspend ::
		delete ::
		get ::
		post ::
		Nil


	private def change (
		params : PostCompaniesByCompanyParams,
		desired : api.Company,
		path : Path
		)
		(implicit global : GlobalEnvironment[F])
		: F[ResultType[api.Company]] =
		for {
			implicit0 (env : ScopedEnvironment[F]) <- createScopedEnvironment (
				path,
				params
				)

			implicit0 (log : StructuredLogger[F]) <- env.loggingFactory
				.create

			_ <- debug"looking up company: ${params.company}"
			existing <- scenarios.findForChange (params)

			_ <- debug"changing existing company: ${existing.id.show} ${existing.version.show}"
			altered <- scenarios.change (existing, desired)

			_ <- debug"saved company: ${altered.id.show} ${altered.version.show}"
			result <- completeF (
				arrows.toApi ()
					.run (altered -> tag[api.Company] (ResourceLocation (path))
					)
				)
			} yield result


	private def changeStatus[ParamsT <: AnyRef] (
		find : FindCompany[F, ParamsT],
		desired : CompanyStatus
		)
		(
			params : ParamsT,
			body : api.VersionOnly,
			path : Path
		)
		(
			implicit
			global : GlobalEnvironment[F],
			correlationId : FindField[ParamsT, Witness.`'X-Correlation-ID`.T, String],
			companyId : FindField[ParamsT, Witness.`'company`.T, String]
		)
		: F[ResultType[api.Company]] =
		for {
			implicit0 (env : ScopedEnvironment[F]) <- createScopedEnvironment (
				path,
				params
				)

			implicit0 (log : StructuredLogger[F]) <- env.loggingFactory
				.create

			_ <- debug"loading '${companyId (params)}' company"
			company <- find (params)

			_ <- debug"changing '${companyId (params)}' company status to '$desired'"
			altered <- scenarios.changeStatus (company, body, desired)
			result <- completeF (
				arrows.toApi ()
					.run (
						altered -> tag[api.Company] (
							ResourceLocation (path.parent)
							)
						)
				)
			} yield result


	private def remove (
		params : DeleteCompaniesByCompanyParams,
		path : Path
		)
		(implicit global : GlobalEnvironment[F])
		: F[ResultType[Unit]] =
		for {
			implicit0 (env : ScopedEnvironment[F]) <- createScopedEnvironment (
				path,
				params
				)

			implicit0 (log : StructuredLogger[F]) <- env.loggingFactory
				.create

			_ <- debug"loading '${params.company}' company"
			company <- scenarios.findForRemove (params)

			_ <- debug"removing '${params.company}' company"
			deleted <- scenarios.remove (company)

			_ <- debug"removed '${params.company}'? $deleted"
			result <- deleted.fold (
				complete (),
				failWith (
					api.ObjectNotFoundDetails
						.from (
							id = params.company,
							status = StatusCode.Gone.code,
							title = "company could not be removed"
							)
					)
				)
			} yield result


	private def retrieve (
		params : GetCompaniesByCompanyParams,
		path : Path
		)
		(implicit global : GlobalEnvironment[F])
		: F[ResultType[api.Company]] =
		for {
			implicit0 (env : ScopedEnvironment[F]) <- createScopedEnvironment (
				path,
				params
				)

			implicit0 (log : StructuredLogger[F]) <- env.loggingFactory.create

			_ <- debug"loading '${params.company}' company"
			company <- scenarios.findForRetrieve (params)
			result <- completeF (
				arrows.toApi ()
					.run (company -> tag[api.Company] (ResourceLocation (path)))
				)
			} yield result
}

