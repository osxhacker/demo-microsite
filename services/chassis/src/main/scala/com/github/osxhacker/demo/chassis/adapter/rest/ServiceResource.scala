package com.github.osxhacker.demo.chassis.adapter.rest

import scala.annotation.implicitNotFound

import cats.{
	Later,
	MonadThrow
	}

import cats.data.Kleisli
import org.typelevel.log4cats.LoggerFactory
import shapeless.Witness
import sttp.model.Uri
import sttp.tapir._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.typelevel.ParamConcat

import com.github.osxhacker.demo.chassis.domain.Specification
import com.github.osxhacker.demo.chassis.domain.algorithm.FindField
import com.github.osxhacker.demo.chassis.effect.{
	Aspect,
	Pointcut,
	ReadersWriterResource
	}

import com.github.osxhacker.demo.chassis.monitoring.metrics.TapirEndpoint


/**
 * The '''ServiceResource''' type defines the contract and common functionality
 * for __all__ service RESTful resources.  Each concrete resource must provide
 * a ''List'' of all supported [[sttp.tapir.server.ServerEndpoint]]s in the
 * order in which the most specific [[sttp.tapir.server.ServerEndpoint]]s are
 * tried __before__ less specific ones.
 */
abstract class ServiceResource[F[_]] ()
	(
		implicit

		/// Needed for `serverLogicWithEnvironment`.
		protected val monadThrow : MonadThrow[F],

		/// Needed for '''TapirEndpoint'''.
		protected val loggerFactory : LoggerFactory[F]
	)
	extends (() => List[ServerEndpoint[Any, F]])
{
	/// Class Imports
	import ServiceResource.EnvironmentUsagePolicy
	import cats.syntax.applicative._
	import cats.syntax.either._
	import cats.syntax.show._


	/// Class Types
	/**
	 * The '''EndpointOps''' type enriches an [[sttp.tapir.Endpoint]] with
	 * behaviour commonly used when defining REST resources.
	 */
	implicit class EndpointOps[SecurityT, InputT, ErrorT, OutputT, R] (
		private val self : Endpoint[SecurityT, InputT, ErrorT, OutputT, R]
		)
	{
		/**
		 * The extractPath method resolves the
		 * [[com.github.osxhacker.demo.chassis.adapter.rest.Path]] associated
		 * with an [[sttp.tapir.Endpoint]] and appends it to ''InputT'' or
		 * `throw`s an error.
		 */
		def extractPath[CombinedT] ()
			(
				implicit
				concat : ParamConcat.Aux[InputT, Path, CombinedT],
				parser : Path.Parser[Uri]
			)
			: Endpoint[SecurityT, CombinedT, ErrorT, OutputT, R] =
			self.in (
				extractFromRequest {
					request =>
						parser (request.uri).valueOr (e => throw e)
					}
				)


		/**
		 * The publishUnderApi method anchors an arbitrary
		 * [[sttp.tapir.Endpoint]] underneath the '''settings''' `http.api`
		 * location.
		 */
		def publishUnderApi ()
			: Endpoint[SecurityT, InputT, ErrorT, OutputT, R] =
			locatedAt (paths.api)


		/**
		 * The serverLogicWithEnvironment method provides the desired ''EnvT''
		 * and ''InputT'' to the given '''handler''' as a `reader` usage if and
		 * only if the '''policy''' allows the invocation to proceed.
		 *
		 * Use `serverLogicWithResource` for situations where a '''policy'''
		 * either is not available or is not semantically meaningful.
		 *
		 * Note that there is no corresponding `writer` combinator provided as
		 * doing so would dictate how those '''handler'''s would have to be
		 * implemented.
		 */
		def serverLogicWithEnvironment[EnvT] (
			handler : EnvT => InputT => F[Either[ErrorT, OutputT]]
			)
			(
				implicit
				@implicitNotFound (
					"an implicit ReadersWriterResource[${F}, ${EnvT}] " +
					"must be in scope to use serverLogicWithEnvironment"
					)
				environment : ReadersWriterResource[F, EnvT],

				@implicitNotFound (
					"an implicit Pointcut[${F}] must be in scope to use " +
					"serverLogicWithEnvironment"
					)
				pointcut : Pointcut[F],

				@implicitNotFound (
					"could not find X-Correlation-ID of type String in ${InputT}"
					)
				findCorrelationId : FindField[
					InputT,
					Witness.`'X-Correlation-ID`.T,
					String
					],

				@implicitNotFound (
					"an implicit EnvironmentUsagePolicy[${F}, ${EnvT}, ${ErrorT}] " +
					"must be in scope to use serverLogicWithEnvironment"
					)
				policy : EnvironmentUsagePolicy[F, EnvT, ErrorT],
				securityIsUnit : SecurityT =:= Unit
			)
			: ServerEndpoint[R, F] =
			self.serverLogic {
				val advice = TapirEndpoint[F, Either[ErrorT, OutputT]] (self) _
				val metrics = Aspect[F, TapirEndpoint[F, Either[ErrorT, OutputT]]].percall (
					(input : InputT) => advice (findCorrelationId (input))
					)

				input => metrics (input) {
					Later (
						environment.reader (
							policy ().andThen (
								_.fold (
									_.asLeft[OutputT].pure[F],
									handler (_) (input)
									)
								)
							)
						)
					}
					.value
				}


		/**
		 * The serverLogicWithResource method provides the desired ''ResourceT''
		 * and ''InputT'' to the given '''handler''' as a `reader` usage.  Note
		 * that there is no corresponding `writer` combinator provided as doing
		 * so would dictate how those '''handler'''s would have to be
		 * implemented.
		 */
		def serverLogicWithResource[ResourceT] (
			handler : ResourceT => InputT => F[Either[ErrorT, OutputT]]
			)
			(
				implicit
				@implicitNotFound (
					"an implicit Pointcut[${F}] must be in scope to use " +
					"serverLogicWithEnvironment"
					)
				pointcut : Pointcut[F],

				@implicitNotFound (
					"could not find X-Correlation-ID of type String in ${InputT}"
					)
				findCorrelationId : FindField[
					InputT,
					Witness.`'X-Correlation-ID`.T,
					String
					],

				@implicitNotFound (
					"an implicit ReadersWriterResource[${F}, ${ResourceT}] " +
					"must be in scope to use serverLogicWithResource"
					)
				resource : ReadersWriterResource[F, ResourceT],
				securityIsUnit : SecurityT =:= Unit
			)
			: ServerEndpoint[R, F] =
			self.serverLogic {
				val advice = TapirEndpoint[F, Either[ErrorT, OutputT]] (self) _
				val metrics = Aspect[F, TapirEndpoint[F, Either[ErrorT, OutputT]]].percall (
					(input : InputT) => advice (findCorrelationId (input))
					)

				input => metrics (input) {
					Later (
						resource.reader (handler (_) (input))
						)
					}
					.value
				}


		private def locatedAt (prefix : Path)
			: Endpoint[SecurityT, InputT, ErrorT, OutputT, R] =
		{
			val segments = prefix.show
				.split ("/")
				.filterNot (_.isEmpty)
				.reverse

			segments.foldLeft (self) {
				case (endpoint, segment) =>
					endpoint.prependIn (segment)
				}
		}
	}


	/**
	 * The '''Paths''' type defines the contract for the required
	 * '''ServiceResource''' path configurations such that every resource
	 * declares its advertised `api`
	 * [[com.github.osxhacker.demo.chassis.adapter.rest.Path]].
	 */
	protected trait Paths
	{
		/// Instance Properties
		def api : Path
	}


	/// Instance Properties
	protected def paths : Paths


	/**
	 * The alwaysAllowEnvironmentUsage method provides syntactic convenience for
	 * creating an
	 * [[com.github.osxhacker.demo.chassis.adapter.rest.ServiceResource.EnvironmentUsagePolicy]]
	 * instance which __always__ allow ''EnvT'' usage.
	 */
	final protected def alwaysAllowEnvironmentUsage[EnvT, ErrorT]
		: EnvironmentUsagePolicy[F, EnvT, ErrorT] =
		new EnvironmentUsagePolicy[F, EnvT, ErrorT] {
			override def apply () : Kleisli[F, EnvT, Either[ErrorT, EnvT]] =
				Kleisli (_.asRight[ErrorT].pure[F])
			}


	/**
	 * The conditionalEnvironmentUsage method provides syntactic convenience
	 * for creating an
	 * [[com.github.osxhacker.demo.chassis.adapter.rest.ServiceResource.EnvironmentUsagePolicy]]
	 * instance which can be decided strictly by an ''EnvT''.
	 */
	final protected def conditionalEnvironmentUsage[EnvT, ErrorT] (
		allow : Specification[EnvT]
		)
		(f : EnvT => ErrorT)
		: EnvironmentUsagePolicy[F, EnvT, ErrorT] =
		new EnvironmentUsagePolicy[F, EnvT, ErrorT] {
			override def apply () : Kleisli[F, EnvT, Either[ErrorT, EnvT]] =
				Kleisli {
					env =>
						Either.cond (allow (env), env, f (env))
							.pure[F]
					}
			}
}


object ServiceResource
{
	/// Class Types
	/**
	 * The '''EnvironmentUsagePolicy''' type is a model of the TYPE CLASS
	 * pattern and defines the contract for determining whether or not
	 * `serverLogicWithEnvironment` should invoke its logic with an instance of
	 * ''EnvT'' based on if the [[cats.data.Kleisli]] produces an ''ErrorT'' or
	 * ''EnvT''.
	 */
	sealed trait EnvironmentUsagePolicy[F[_], EnvT, ErrorT]
	{
		def apply () : Kleisli[F, EnvT, Either[ErrorT, EnvT]]
	}
}

