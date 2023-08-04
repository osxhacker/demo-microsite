package com.github.osxhacker.demo.gatling

import java.util.Base64

import io.gatling.core.CoreDsl
import io.gatling.http.HttpDsl
import io.gatling.http.request.builder.HttpRequestBuilder


trait AuthenticationAware[KeyT <: SessionKey]
{
	/// Self Type Constraints
	this : CoreDsl
		with HttpDsl
		=>


	/// Class Types
	/**
	 * The '''AuthenticationSyntax''' type extends
	 * [[io.gatling.http.request.builder.HttpRequestBuilder]] to support
	 * managing and sending
	 * [[https://datatracker.ietf.org/doc/html/rfc6750 Bearer Tokens]] in a
	 * request.
	 */
	implicit class AuthenticationSyntax (private val self : HttpRequestBuilder)
	{
		def bearerAuth (key : KeyT) : HttpRequestBuilder =
			self.header (
				HttpHeaderNames.Authorization,
				session =>
					"Bearer %s".format (
						encode (session (key.entryName).as[String])
						)
				)


		def bearerAuth (token : String) : HttpRequestBuilder =
			self.header (
				HttpHeaderNames.Authorization,
				s"Bearer ${encode (token)}"
				)


		def saveBearerToken (key : KeyT) : HttpRequestBuilder =
			self.check (
				header (HttpHeaderNames.Authorization)
					.find
					.transform (decode)
					.saveAs (key.entryName)
				)


		private def decode (token : String) : String =
			new String (
				Base64.getDecoder
					.decode (tokenOnly (token)),

				"UTF-8"
				)


		private def encode (token : String) : String =
			Base64.getEncoder
				.encodeToString (tokenOnly (token).getBytes)


		private def tokenOnly (candidate : String) : String =
			candidate.trim
				.stripPrefix ("Bearer")
				.trim
	}
}
