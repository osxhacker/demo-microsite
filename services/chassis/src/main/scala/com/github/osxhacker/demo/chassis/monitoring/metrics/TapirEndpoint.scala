package com.github.osxhacker.demo.chassis.monitoring.metrics

import scala.language.postfixOps

import kamon.context.Context
import sttp.model.Method
import sttp.tapir.AnyEndpoint

import com.github.osxhacker.demo.chassis.effect.DefaultAdvice


/**
 * The '''TapirEndpoint''' type defines the standard
 * [[com.github.osxhacker.demo.chassis.monitoring.metrics.MetricsAdvice]]
 * intended for use in measuring [[sttp.tapir.Endpoint]]s.
 */
final class TapirEndpoint[F[_], ResultT] private (
	private val method : Method,
	private val location : String,
	private val correlationId : String
	)
	extends DefaultAdvice[F, ResultT] ()
		with MetricsAdvice[F, ResultT]
		with InvocationCounters[F, ResultT]
		with StartWorkflow[F, ResultT]
{
	/// Class Imports
	import TapirEndpoint.keys


	/// Instance Properties
	override val component = "tapir"
	override val group = subgroup (
		super.group,
		"rest"
		)

	override val initialContext =
		Context.of (keys.correlationId, correlationId)
			.withEntry (keys.httpMethod, method)
			.withEntry (keys.httpUrl, location)

	override val operation =
		new StringBuilder ()
			.append (method.toString ().toUpperCase ())
			.append (' ')
			.append (location)
			.toString ()

	override val tags =
		super.tags
			.withTag (keys.httpMethod.name, method.method)
			.withTag (keys.httpUrl.name, location)
}


object TapirEndpoint
{
	/// Class Types
	private object keys
	{
		val correlationId : Context.Key[String] =
			Context.key[String] ("correlationId", "undefined")

		val httpMethod : Context.Key[Method] =
			Context.key ("http.method", Method.GET)

		val httpUrl : Context.Key[String] =
			Context.key ("http.url", "undefined")
	}


	/**
	 * The apply method is provided to support functional-style creation of
	 * '''TapirEndpoint''' instances.
	 */
	def apply[F[_], ResultT] (endpoint : AnyEndpoint)
		(correlationId : String)
		: TapirEndpoint[F, ResultT] =
		new TapirEndpoint[F, ResultT] (
			endpoint.method.getOrElse (Method.GET),
			endpoint.showPathTemplate (),
			correlationId
			)
}

