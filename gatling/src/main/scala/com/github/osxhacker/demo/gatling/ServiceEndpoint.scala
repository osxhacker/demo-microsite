package com.github.osxhacker.demo.gatling

import scala.language.implicitConversions
import scala.sys.SystemProperties

import java.net.URI

import io.gatling.core.session.Expression


/**
 * The '''ServiceEndpoint''' type reifies the concept of the specific location
 * of a microservice.  It also supports the ability to produce a more specific
 * endpoint based on a refining sub-path.
 */
final case class ServiceEndpoint (private val uri : URI)
{
	override val toString : String = uri.toASCIIString


	/**
	 * The slash method allows '''this''' instance to append to its path the
	 * given '''subPath'''.
	 */
	def / (subPath : String) : ServiceEndpoint =
		ServiceEndpoint (
			new URI (
				uri.getScheme,
				uri.getUserInfo,
				uri.getHost,
				uri.getPort,
				Option (uri.getPath).map (_.stripSuffix ("/"))
					.getOrElse ("") + "/" + subPath.stripPrefix ("/"),
				uri.getQuery,
				uri.getFragment
				)
			)


	/**
	 * The question mark method allows '''this''' instance to __replace__ the
	 * query string associated with the underlying `uri`.
	 */
	def ? (query : Map[String, Any]) : ServiceEndpoint =
		ServiceEndpoint (
			new URI (
				uri.getScheme,
				uri.getUserInfo,
				uri.getHost,
				uri.getPort,
				uri.getPath,
				query.mkString ("?", "&", ""),
				uri.getFragment
				)
			)
}


object ServiceEndpoint
{
	/// Class Imports
	import io.gatling.core.session._


	/// Instance Properties
	private val properties = new SystemProperties ()


	/**
	 * This version of the apply method is provided to allow functional-style
	 * creation of a '''ServiceEndpoint''' from a '''raw'''
	 * [[https://www.rfc-editor.org/rfc/rfc3986 URI]] ''String''.
	 */
	def apply (raw : String) : ServiceEndpoint = ServiceEndpoint (new URI (raw))


	/**
	 * This version of the apply method is provided to allow functional-style
	 * creation of a '''ServiceEndpoint''' from a raw
	 * [[https://www.rfc-editor.org/rfc/rfc3986 URI]] ''String'' found in a
	 * system property named '''propertyName''' __or__ the '''default''' given.
	 */
	def apply (propertyName : String, default : String) : ServiceEndpoint =
		apply (properties.getOrElse (propertyName, default))


	/// Implicit Conversions
	implicit def serviceEndpointToString (instance : ServiceEndpoint) : String =
		instance.toString


	implicit def serviceEndpointToExpression (instance : ServiceEndpoint)
		: Expression[String] =
		instance.toString
			.expressionSuccess
}

