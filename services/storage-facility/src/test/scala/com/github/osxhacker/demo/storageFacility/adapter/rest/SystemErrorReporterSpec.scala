package com.github.osxhacker.demo.storageFacility.adapter.rest

import cats.data.NonEmptyChain
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec
import sttp.client3._
import sttp.model.StatusCode
import sttp.tapir.{
	ValidationError => _,
	_
	}

import sttp.tapir.server.interceptor.exception.ExceptionContext
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.server.stub.SttpRequest

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.chassis.adapter.rest.TapirClientSupport
import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.chassis.domain.entity._
import com.github.osxhacker.demo.chassis.domain.error._
import com.github.osxhacker.demo.storageFacility.domain.StorageFacility


/**
 * The '''SystemErrorReporterSpec''' type defines the unit-tests which
 * certify
 * [[com.github.osxhacker.demo.storageFacility.adapter.rest.SystemErrorReporter]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class SystemErrorReporterSpec ()
	extends AnyWordSpec
		with Diagrams
		with ProjectSpec
{
	/// Class Imports
	import cats.syntax.all._


	/// Class Types
	type ResponseType = (api.ProblemDetails, StatusCode)


	object ResponseType
	{
		def unapply (candidate : Any) : Option[ResponseType] =
			candidate match {
				case (details : api.ProblemDetails, code : StatusCode) =>
					Some (details -> code)

				case _ =>
					None
			}
	}


	/// Instance Properties
	implicit private val errorOrShim =
		new TapirClientSupport.SttpMonadErrorShim[ErrorOr] ()

	private val errorReporter = SystemErrorReporter[ErrorOr] ()
	private val mockRequest = SttpRequest (quickRequest.get (uri"/test"))
	private val serverEndpoint : AnyEndpoint =
		endpoint.get
			.in ("test")
			.out (stringBody)


	"The SystemErrorReporter" must {
		"handle illegal argument exceptions" in {
			reportOn (StatusCode.BadRequest) (
				new IllegalArgumentException ("test")
				)
			}

		"handle object conflict errors" in {
			reportOn (StatusCode.Conflict) (
				ConflictingObjectsError[StorageFacility] (
					"test"
					)
				)
			}

		"handle domain value errors" in {
			reportOn (StatusCode.BadRequest) (
				DomainValueError ("test")
				)
			}

		"handle invalid model state errors" in {
			reportOn (StatusCode.UnprocessableEntity) (
				InvalidModelStateError[StorageFacility] (
					id = Identifier.fromRandom[StorageFacility] (),
					version = Version.initial,
					message = "invalid model state test"
					)
				)
			}

		"handle logic errors" in {
			reportOn (StatusCode.InternalServerError) (
				LogicError ("logic error test")
				)
			}

		"handle object not found errors" in {
			reportOn (StatusCode.NotFound) (
				ObjectNotFoundError[StorageFacility] (
					id = Identifier.fromRandom[StorageFacility] ()
					)
				)
			}

		"handle stale object errors" in {
			reportOn (StatusCode.Conflict) (
				StaleObjectError[StorageFacility] (
					id = Identifier.fromRandom[StorageFacility] (),
					version = Version.initial
					)
				)
			}

		"handle unknown persistence errors" in {
			reportOn (StatusCode.InternalServerError) (
				UnknownPersistenceError ("unknown persistence test")
				)
			}

		"handle validation errors" in {
			reportOn (StatusCode.BadRequest) (
				ValidationError[StorageFacility] (
					"validation test".pure[NonEmptyChain]
					)
				)
			}

		"handle unknown errors" in {
			reportOn (StatusCode.InternalServerError) (
				new IllegalAccessError("test")
				)
		}
	}


	private def reportOn[A <: Throwable] (expected : StatusCode)
		(error : A)
		: Option[ValuedEndpointOutput[_]] =
	{
		val context = ExceptionContext (error, serverEndpoint, mockRequest)
		val result = errorReporter (context).orFail ()

		assert (result.isDefined)
		result foreach {
			case ValuedEndpointOutput (_, ResponseType (details, code)) =>
				assert (details.status.value == code.code)
				assert (code === expected)
				assert (details.title.value.trim () !== "")

			case other =>
				fail ("problem details were not produced: " + other)
		}

		result
	}
}
