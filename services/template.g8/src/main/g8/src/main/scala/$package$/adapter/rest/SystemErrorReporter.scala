package $package$.adapter.rest

import eu.timepit.refined
import monocle.Getter
import sttp.model.StatusCode
import sttp.tapir.generic.auto._

import com.github.osxhacker.demo.chassis.adapter.rest.AbstractErrorReporter
import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.chassis.domain.entity.Version
import com.github.osxhacker.demo.chassis.domain.error._
import $package$.adapter.rest.api._


/**
 * The '''SystemErrorReporter''' type completes the
 * [[com.github.osxhacker.demo.chassis.adapter.rest.AbstractErrorReporter]]
 * contract by manufacturing relevant
 * [[$package$.adapter.rest.api.ProblemDetails]]
 * instances within the container ''F'' when requested.
 */
final case class SystemErrorReporter[F[_]] ()
	extends AbstractErrorReporter[F, ProblemDetails] ()
{
	/// Class Imports
	import cats.syntax.option._
	import refined.auto._


	/// Instance Properties
	override protected val fallback = ServiceImplementationDetails (
		title = "unable to create a ProblemDetails from an exception"
		)

	override protected val statusCodeLens = Getter[ProblemDetails, StatusCode] (
		pd => StatusCode (pd.status.value)
		)


	override protected def badRequest (error : DomainValueError)
		: ErrorOr[ProblemDetails] =
		BadRequestDetails.from (
			title = "a domain value error was detected",
			detail = error.getMessage
				.some
			)


	override protected def badRequest (error : IllegalArgumentException)
		: ErrorOr[ProblemDetails] =
		BadRequestDetails.from (
			title = "an illegal value was detected",
			detail = error.getMessage
				.some
			)


	override protected def badRequest (error : ValidationError[_])
		: ErrorOr[ProblemDetails] =
		BadRequestDetails.from (
			title = "request validation failed",
			detail = error.getMessage
				.some
			)


	override protected def conflict (error : ConflictingObjectsError[_])
		: ErrorOr[ProblemDetails] =
		MinimalProblemDetails.from (
			status = StatusCode.Conflict.code,
			title = "resource conflict detected",
			detail = error.getMessage
				.some
			)


	override protected def conflict (error : DuplicateObjectError[_])
		: ErrorOr[ProblemDetails] =
		MinimalProblemDetails.from (
			status = StatusCode.Conflict.code,
			title = "duplicate resource detected",
			detail = error.getMessage
				.some
			)


	override protected def invalidModelState (error : InvalidModelStateError[_])
		: ErrorOr[ProblemDetails] =
		InvalidModelStateDetails.from (
			title = "the requested operation cannot be completed",
			detail = error.getMessage
				.some
			)


	override protected def unknownError (error : Throwable)
		: ErrorOr[ProblemDetails] =
		ServiceImplementationDetails.from (
			title = "unexpected service error",
			detail = error.getMessage
				.some
			)


	override protected def logicError(error : LogicError)
		: ErrorOr[ProblemDetails] =
		ServiceImplementationDetails.from (
			title = "service implementation error",
			detail = error.getMessage
				.some
		)


	override protected def objectNotFound (error : ObjectNotFoundError[_])
		: ErrorOr[ProblemDetails] =
		ObjectNotFoundDetails.from (
			title = "required object not found",
			id = error.id
				.toUrn (),

			detail = error.getMessage
				.some
				)


	override protected def persistence (error : UnknownPersistenceError)
		: ErrorOr[ProblemDetails] =
		MinimalProblemDetails.from (
			status = StatusCode.InternalServerError.code,
			title = error.getMessage,
			detail = error.cause
				.map (_.getMessage)
			)


	override protected def staleObject (error : StaleObjectError[_])
		: ErrorOr[ProblemDetails] =
		StaleObjectDetails.from (
			title = "submitted resource is outdated",
			id = error.id
				.toUrn (),

			submittedVersion = Version.value
				.get (error.version)
			)
}

