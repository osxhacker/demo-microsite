package com.github.osxhacker.demo.gatling

import scala.collection.immutable.ListMap
import scala.jdk.CollectionConverters._

import io.gatling.core.CoreDsl
import io.gatling.core.session.{
	Expression,
	Session
	}

import io.gatling.http.HttpDsl
import io.gatling.http.request.builder.HttpRequestBuilder
import io.gatling.http.response.Response
import org.slf4j.LoggerFactory


/**
 * The '''LoggingAware''' type defines
 * [[https://gatling.io/docs/gatling/tutorials/quickstart/ Gatling]] extensions
 * related to using [[org.slf4j]]-based logging.
 */
trait LoggingAware
{
	/// Self Type Constraints
	this : CoreDsl
		with HttpDsl
		=>


	/// Class Types
	/**
	 * The '''LoggingSyntax''' type extends
	 * [[io.gatling.http.request.builder.HttpRequestBuilder]] to support
	 * emitting [[org.slf4j.Logger]]-based messages during the evaluation of
	 * '''self'''.
	 */
	implicit class LoggingSyntax (private val self : HttpRequestBuilder)
	{
		/// Class Imports
		import io.gatling.commons.validation._


		/**
		 * The debug method unconditionally logs the ''String'' produced by
		 * '''f''' at the "DEBUG" level.
		 */
		def debug (f : (Response, Session) => String) : HttpRequestBuilder =
			self.transformResponse {
				(response, session) =>
					logger.debug (f (response, session))

					response.success
				}


		/**
		 * The info method unconditionally logs the ''String'' produced by
		 * '''f''' at the "INFO" level.
		 */
		def info (f : (Response, Session) => String) : HttpRequestBuilder =
			self.transformResponse {
				(response, session) =>
					logger.info (f (response, session))

					response.success
				}


		/**
		 * The logResponse method produces a `debug` log entry consisting of
		 * the [[io.gatling.http.client.Request]] URI as well as the contents
		 * of the latest [[io.gatling.http.response.Response]].
		 */
		def logResponse () : HttpRequestBuilder =
			debug {
				(response, _) =>
					val headers = response.headers
						.asScala
						.map {
							kvp =>
								kvp.getKey -> kvp.getValue
							}
						.to (ListMap)
						.updated ("Status", response.status.code ().toString)

					new StringBuilder ()
						.append (response.request.getMethod.toString)
						.append (' ')
						.append (response.request.getUri)
						.append (" response:")
						.append (pretty (headers))
						.append ('\n')
						.append (response.body.string)
						.toString ()
				}


		/**
		 * The warnIf method invokes '''f''' to log at the "WARN" level iff the
		 * given '''guard''' returns `true`.
		 */
		def warnIf (guard : (Response, Session) => Boolean)
			(f : (Response, Session) => String)
			: HttpRequestBuilder =
			self.transformResponse {
				(response, session) =>
					if (guard (response, session))
						logger.warn (f (response, session))

					response.success
				}
	}


	/// Instance Properties
	private val logger = LoggerFactory.getLogger (getClass)


	protected def logSession () : Expression[Session] =
		logSession ("session:")


	protected def logSession (label : String) : Expression[Session] =
		session => {
			logger.debug ("{}{}", label, pretty (session.attributes))

			session
			}


	private def pretty (map : Map[String, Any]) : String =
	{
		val width = map.keySet
			.map (_.length)
			.max

		val layout = s"%${width}s -> %s"

		map.foldLeft (new StringBuilder ()) {
			case (accum, kvp) =>
				accum.append ("\n\t")
					.append (layout.format (kvp._1, kvp._2))
			}
			.toString ()
	}
}

