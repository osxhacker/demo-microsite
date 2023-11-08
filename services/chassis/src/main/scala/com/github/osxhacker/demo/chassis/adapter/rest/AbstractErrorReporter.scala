package com.github.osxhacker.demo.chassis.adapter.rest

import io.circe.{
	Decoder,
	Encoder
	}

import monocle.Getter
import sttp.model.StatusCode
import sttp.monad.MonadError
import sttp.tapir
import sttp.tapir.Schema
import sttp.tapir.server.interceptor.exception.{
	ExceptionContext,
	ExceptionHandler
	}

import sttp.tapir.server.model.ValuedEndpointOutput

import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.chassis.domain.error._


/**
 * The '''AbstractErrorReporter''' type is a model of the TEMPLATE pattern and
 * defines the ability to interpret [[sttp.tapir]] errors in order to produce
 * ''ProblemDetailsT'' payloads.  Since the code generated for REST API's are
 * expected to be in an open ended set of `package`s, concrete instances of
 * '''AbstractErrorReporter''' are responsible for instantiating the proper
 * ''ProblemDetailsT'' based on which method is invoked.
 *
 * Each concrete implementation of '''AbstractErrorReporter''' can introduce
 * handing of service-specific ''ProblemDetailT''s by providing their own
 * `service` [[scala.PartialFunction]] property.
 *
 * @see [[https://tapir.softwaremill.com/en/latest/server/errors.html Tapir Errors]]
 */
abstract class AbstractErrorReporter[F[_], ProblemDetailsT <: AnyRef] ()
	(
		implicit

		/// Needed for `jsonBody`.
		private val decoder : Decoder[ProblemDetailsT],
		private val encoder : Encoder[ProblemDetailsT],
		private val schema : Schema[ProblemDetailsT]
	)
	extends ExceptionHandler[F]
{
	/// Class Imports
	import cats.syntax.either._
	import cats.syntax.functor._
	import cats.syntax.option._
	import tapir.statusCode
	import tapir.json.circe.jsonBody


	/// Class Types
	final protected type HandlerType = PartialFunction[
		Throwable,
		ErrorOr[ProblemDetailsT]
		]

	private type DefaultDomainType = tapir.Endpoint[_, _, _, _, _]


	/// Instance Properties
	protected def fallback : ProblemDetailsT
	protected val statusCodeLens : Getter[ProblemDetailsT, StatusCode]
	protected def service : HandlerType = PartialFunction.empty

	private val default : HandlerType =
	{
		case coe : ConflictingObjectsError[_] =>
			conflict (coe).leftFlatMap (unknownError)

		case dve : DomainValueError =>
			badRequest (dve).leftFlatMap (unknownError)

		case doe : DuplicateObjectError[_] =>
			conflict (doe).leftFlatMap (unknownError)

		case imse : InvalidModelStateError[_] =>
			invalidModelState (imse).leftFlatMap (unknownError)

		case le : LogicError =>
			logicError (le).leftFlatMap (unknownError)

		case onfe : ObjectNotFoundError[_] =>
			objectNotFound (onfe).leftFlatMap (unknownError)

		case soe : StaleObjectError[_] =>
			staleObject (soe).leftFlatMap (unknownError)

		case upe : UnknownPersistenceError =>
			persistence (upe).leftFlatMap (unknownError)

		case ve : ValidationError[_] =>
			badRequest (ve).leftFlatMap (unknownError)

		case iae : IllegalArgumentException =>
			badRequest (iae).leftFlatMap (unknownError)
	}

	private lazy val handlers = service orElse default


	/// Abstract Methods
	protected def badRequest (error : DomainValueError)
		: ErrorOr[ProblemDetailsT]


	protected def badRequest (error : IllegalArgumentException)
		: ErrorOr[ProblemDetailsT]


	protected def badRequest (error : ValidationError[_])
		: ErrorOr[ProblemDetailsT]


	protected def conflict (error : ConflictingObjectsError[_])
		: ErrorOr[ProblemDetailsT]


	protected def conflict (error : DuplicateObjectError[_])
		: ErrorOr[ProblemDetailsT]


	protected def invalidModelState (error : InvalidModelStateError[_])
		: ErrorOr[ProblemDetailsT]


	protected def logicError (error : LogicError) : ErrorOr[ProblemDetailsT]


	protected def objectNotFound (error : ObjectNotFoundError[_])
		: ErrorOr[ProblemDetailsT]


	protected def persistence (error : UnknownPersistenceError)
		: ErrorOr[ProblemDetailsT]


	protected def staleObject (error : StaleObjectError[_])
		: ErrorOr[ProblemDetailsT]


	protected def unknownError (error : Throwable) : ErrorOr[ProblemDetailsT]


	final override def apply (ctx : ExceptionContext)
		(implicit monad : MonadError[F])
		: F[Option[ValuedEndpointOutput[_]]] =
	{
		val problemDetails = handlers.applyOrElse (ctx.e, unknownError)
			.fproduct (statusCodeLens.get)
			.getOrElse (fallback -> statusCodeLens.get (fallback))

		monad.unit (
			ValuedEndpointOutput (
				jsonBody[ProblemDetailsT].and (statusCode),
				problemDetails
				)
				.some
			)
	}


	final def defaultFailureResponse (message : String)
		: ValuedEndpointOutput[_] =
	{
		val problemDetails = badRequest (
			ValidationError[DefaultDomainType] (message)
			)
			.fproduct (statusCodeLens.get)
			.getOrElse (fallback -> statusCodeLens.get (fallback))

		ValuedEndpointOutput (
			jsonBody[ProblemDetailsT].and (statusCode),
			problemDetails
			)
	}
}

