package com.github.osxhacker.demo.gatling

import java.util.UUID.randomUUID

import io.gatling.core.CoreDsl
import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.HttpDsl
import io.gatling.http.request.builder.HttpRequestBuilder


/**
 * The '''Task''' type defines a common ancestor for atomic
 * [[https://gatling.io/docs/gatling/tutorials/quickstart/ Gatling]]-based
 * interactions with a service.  Each '''Task''' should support composition via
 * the `exec` method.  Note that the `configuration` property __must__ be
 * available for use by [[io.gatling.core.CoreDsl]].
 *
 * @see [[https://gatling.io/docs/gatling/reference/current/core/scenario/#exec]]
 */
abstract class Task[KeyT <: SessionKey] ()
	(implicit override val configuration : GatlingConfiguration)
	extends CoreDsl
		with HttpDsl
		with LoggingAware
		with StatusAware
		with AuthenticationAware[KeyT]
		with CirceAware[KeyT]
{
	/// Class Imports
	import io.gatling.commons.validation._


	/// Class Types
	/**
	 * The '''CorrelationIdSyntax''' type extends
	 * [[io.gatling.http.request.builder.HttpRequestBuilder]] to support adding
	 * the "X-Correlation-ID" header to '''self''' having a `randomUUID` as its
	 * value.
	 */
	implicit class CorrelationIdSyntax (private val self : HttpRequestBuilder)
	{
		def addCorrelationId () =
			self.header (
				`X-Correlation-ID`,
				_ =>
					randomUUID ().toString
						.success
				)
	}


	/// Instance Properties
	protected val `X-Correlation-ID` : String = "X-Correlation-ID"
}

