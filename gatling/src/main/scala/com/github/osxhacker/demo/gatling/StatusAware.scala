package com.github.osxhacker.demo.gatling

import io.gatling.core.CoreDsl
import io.gatling.http.HttpDsl


/**
 * The '''StatusAware''' type defines
 * [[https://gatling.io/docs/gatling/tutorials/quickstart/ Gatling]]-related
 * [[https://developer.mozilla.org/en-US/docs/Web/HTTP/Status HTTP Status Code]]
 * checks.
 */
trait StatusAware
{
	/// Self Type Constraints
	this : CoreDsl
		with HttpDsl
		=>


	/// Instance Properties
	final protected val isClientError = status.in (400 until 500)
	final protected val isCreated = status.is (201)
	final protected val isError = status.in (400 until 600)
	final protected val isOk = status.is (200)
	final protected val isRedirect = status.in (200 until 400)
	final protected val isServerError = status.in (500 until 600)
	final protected val isSuccess = status.in (200 until 300)
}

