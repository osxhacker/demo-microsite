package com.github.osxhacker.demo.company.adapter.rest

import scala.annotation.implicitNotFound

import cats.MonadThrow
import org.typelevel.log4cats.LoggerFactory
import shapeless.Witness
import sttp.model.StatusCode

import com.github.osxhacker.demo.chassis.adapter.rest.{
	Path,
	ServiceResource
	}

import com.github.osxhacker.demo.chassis.effect.ReadersWriterResource
import com.github.osxhacker.demo.chassis.domain.algorithm.FindField
import com.github.osxhacker.demo.chassis.monitoring.Subsystem
import com.github.osxhacker.demo.company.adapter.RuntimeSettings
import com.github.osxhacker.demo.company.domain.{
	GlobalEnvironment,
	ScopedEnvironment
	}

import api.{
	LogicErrorDetails,
	ProblemDetails,
	ServiceUnavailableDetails
	}


/**
 * The '''AbstractResource''' type partially fulfills the
 * [[com.github.osxhacker.demo.chassis.adapter.rest.ServiceResource]] contract
 * and, as such, is the common ancestor to __all__
 * [[com.github.osxhacker.demo.company.adapter.rest]] resources.
 */
abstract class AbstractResource[F[_]] ()
	(
		implicit
		/// Needed for `complete` and `failWith`.
		override protected val monadThrow : MonadThrow[F],

		/// Needed for `log4cats.syntax`
		protected val loggerFactory : LoggerFactory[F]
	)
	extends ServiceResource[F] ()
{
	/// Class Imports
	import cats.syntax.applicative._
	import cats.syntax.either._
	import cats.syntax.functor._
	import cats.syntax.show._
	import mouse.any._


	/// Class Types
	final type ErrorType = (Option[ProblemDetails], StatusCode)


	/**
	 * The '''ResultType''' `type` provides a consistent definition for the
	 * majority of [[sttp.tapir.Endpoint]] results.  When there is an error,
	 * a ''Tuple2'' with an optional
	 * [[com.github.osxhacker.demo.company.adapter.rest.api.ProblemDetails]]
	 * and required [[sttp.model.StatusCode]] provided.  The "happy path" is
	 * yielded by way of an arbitrary ''A'' instance.
	 */
	final type ResultType[A] = Either[ErrorType, A]


	object paths
		extends Paths
	{
		override val api = Path (settings.http.api)
	}


	/// Instance Properties
	implicit protected def environment
		: ReadersWriterResource[F, GlobalEnvironment[F]]

	implicit protected def environmentPolicy =
		conditionalEnvironmentUsage[GlobalEnvironment[F], ErrorType] (_.isOnline) {
			env =>
				ServiceUnavailableDetails.from (
					title = env.shutdownMessage
						.getOrElse ("company is offline")
					) |>
					mapIntoErrorType
			}

	implicit final protected val subsystem = Subsystem ("rest")

	protected def settings : RuntimeSettings


	/**
	 * The createScopedEnvironment method provides endpoint methods with a
	 * convenient, consistent, mechanism for creating a
	 * [[com.github.osxhacker.demo.company.domain.ScopedEnvironment]] from a
	 * [[com.github.osxhacker.demo.company.domain.GlobalEnvironment]] and
	 * endpoint-specific collaborators.
	 */
	final protected def createScopedEnvironment[ParamsT] (
		path : Path,
		params : ParamsT
		)
		(
			implicit
			@implicitNotFound ("could not find the GlobalEnvironment[${F}]")
			global : GlobalEnvironment[F],

			@implicitNotFound (
				"could not find X-Correlation-ID of type String in ${ParamsT}"
				)
			findCorrelationId : FindField[
				ParamsT,
				Witness.`'X-Correlation-ID`.T,
				String
				]
		)
		: F[ScopedEnvironment[F]] =
		global.scopeWith (findCorrelationId (params))
			.map (_.addContext (Map ("url.path" -> path.show)))


	/**
	 * This version of the complete method indicates successful
	 * [[sttp.tapir.Endpoint]] execution when the result type is ''Unit''.
	 */
	final protected def complete () : F[ResultType[Unit]] =
		completeF (monadThrow.unit)


	/**
	 * This version of the complete method indicates successful
	 * [[sttp.tapir.Endpoint]] execution when the result type is an arbitrary
	 * ''A'' instance.
	 */
	final protected def complete[A] (response : A) : F[ResultType[A]] =
		completeF[A] (response.pure[F])


	/**
	 * The completeF method indicates successful [[sttp.tapir.Endpoint]]
	 * execution when the result type is an arbitrary ''A'' instance within the
	 * container ''F''.
	 */
	final protected def completeF[A] (response : F[A]) : F[ResultType[A]] =
		response.map (_.asRight)


	/**
	 * This version of failWith lifts '''problem''' into ''F[ ResultType[A] ]''.
	 */
	final protected def failWith[A] (problem : ProblemDetails)
		: F[ResultType[A]] =
		(Option (problem) -> StatusCode (problem.status.value)).asLeft[A]
			.pure[F]


	/**
	 * This version of the failWith method produces '''problem''' within
	 * ''F[ ResultType[A] ]''.  Typical usage is to detect when values given to
	 * a [[com.github.osxhacker.demo.company.adapter.rest.api.ProblemDetails]]
	 * derived type's `from` method do not conform to its contract.
	 */
	final protected def failWith[A] (
		problem : Either[IllegalArgumentException, ProblemDetails]
		)
		: F[ResultType[A]] =
		failWithF[A] (problem.pure[F])


	/**
	 * The failWithF method creates a ''ResultType[A]'' from a '''problem''',
	 * both within ''F''.  Should '''problem''' itself be in error, a
	 * [[com.github.osxhacker.demo.company.adapter.rest.api.LogicErrorDetails]]
	 * will be emitted.
	 */
	final protected def failWithF[A] (
		problem : F[Either[IllegalArgumentException, ProblemDetails]]
		)
		: F[ResultType[A]] =
		problem.map (mapIntoErrorType (_).asLeft)


	private def logicError (error : IllegalArgumentException)
		: (Option[ProblemDetails], StatusCode) =
		(
			Option (
				LogicErrorDetails (
					title = LogicErrorDetails.TitleType.unsafeFrom (
						s"Logic error detected in: ${getClass.getSimpleName}"
						),

					detail = LogicErrorDetails.DetailType.unsafeFrom (
						Option (error.getMessage)
						)
					)
				),

			StatusCode.InternalServerError
		)


	private def mapIntoErrorType (
		problem : Either[IllegalArgumentException, ProblemDetails]
		)
		: ErrorType =
		problem.fold (
			logicError,
			pd => Option (pd) -> StatusCode (pd.status.value)
			)
}

