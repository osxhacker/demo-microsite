package com.github.osxhacker.demo.gatling

import scala.annotation.{
	implicitNotFound,
	unused
	}

import scala.language.implicitConversions
import scala.reflect.ClassTag

import io.circe.{
	Decoder,
	Encoder
	}

import io.circe.parser._
import io.gatling.core.CoreDsl
import io.gatling.core.body.StringBody
import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.HttpDsl
import io.gatling.http.request.builder.HttpRequestBuilder


/**
 * The '''CirceAware''' type defines support for [[io.circe]] use within
 * [[io.gatling.core.scenario.Simulation]]s.  This is primarily done with
 * extension `class`es and `implicit` conversions.
 */
trait CirceAware[KeyT <: SessionKey]
{
	/// Self Type Constraints
	this : CoreDsl
		with HttpDsl
		=>


	/// Class Imports
	import io.gatling.core.session._


	/// Class Types
	/**
	 * The '''JsonSyntax''' type extends arbitrary ''A'' instances with the
	 * ability to produce ''String''s having JSON content.
	 */
	implicit class JsonSyntax[A] (private val self : A)
	{
		/**
		 * The asJsonString method provides syntactic sugar for converting
		 * '''self''' into a ''String'' containing its JSON representation.
		 */
		def asJsonString ()
			(implicit encoder : Encoder[A])
			: String =
			encoder (self).toString ()


		/**
		 * The asStringBody method is a named version of the
		 * `circeAwareToStringBody` `implicit` conversion.
		 */
		def asStringBody ()
			(implicit encoder: Encoder[A])
			: StringBody =
			circeAwareToStringBody[A] (self)
	}


	/**
	 * The '''MapToSyntax''' type extends
	 * [[io.gatling.http.request.builder.HttpRequestBuilder]] to support using a
	 * [[io.circe.Decoder]] to `parse` the response as an arbitrary type ''A'',
	 * then store it within the Gatling session.
	 */
	implicit class MapToSyntax (private val self : HttpRequestBuilder)
	{
		/**
		 * The mapTo method attempts to `parse` the `bodyString` into an
		 * arbitrary type ''A'' using the `implicit` '''decoder'''.  In the
		 * event of a [[io.circe.Decoder]] failure, the problem will be
		 * represented as a [[io.gatling.commons.validation.Failure]].
		 *
		 * Note that this method definition intentionally does not have a
		 * nullary argument list.  This is so it can employ the "partially
		 * applied" idiom.
		 *
		 * @see [[com.github.osxhacker.demo.gatling.CirceAware.PartiallyAppliedMapTo]]
		 */
		def mapTo[A <: AnyRef] : CirceAware.PartiallyAppliedMapTo[A, KeyT] =
			new CirceAware.PartiallyAppliedMapTo[A, KeyT] (self)
	}


	/// Implicit Conversions
	implicit def circeAwareToStringBody[A] (instance : A)
		(
			implicit
			configuration : GatlingConfiguration,
			encoder : Encoder[A]
		)
		: StringBody =
		new StringBody (
			encoder (instance).toString ().expressionSuccess,
			configuration.core.charset
			)
}


object CirceAware
{
	/// Class Imports
	import io.gatling.commons.validation._


	/// Class Types
	sealed trait LowerPriorityApply[PayloadT <: AnyRef, KeyT <: SessionKey]
		extends CoreDsl
			with HttpDsl
	{
		/// Instance Properties
		protected def builder : HttpRequestBuilder


		/**
		 * This version of the apply method uses run-time meta-information to
		 * ensure the ''PayloadT'' is compatible with the given '''key'''.
		 */
		def apply[K <: KeyT] (key : K)
			(
				implicit
				payloadClassTag : ClassTag[PayloadT],
				decoder : Decoder[PayloadT],

				@implicitNotFound (
					"${K} does not have ${PayloadT} as its ValueType."
					)
				@unused
				ev : K =:= KeyT
			)
			: HttpRequestBuilder =
			builder.check {
				bodyString.transformOption (
					_.fold[Validation[Option[key.ValueType]]] (
						"missing JSON body".failure
						) {
						parse (_).flatMap (decoder.decodeJson)
							.flatMap (key.convert[PayloadT])
							.map (Option (_))
							.toTry
							.toValidation
						}
					)
					.saveAs (key.entryName)
			}
	}


	/**
	 * The '''PartiallyAppliedMapTo''' type completes the "partially applied"
	 * idiom when using the `mapTo` extension method.  The two `apply` methods
	 * allow for optimal enforcement of
	 * [[com.github.osxhacker.demo.gatling.SessionKey]] use, specifically by
	 * ensuring ''ValueType'' conformance as early as possible.
	 */
	final class PartiallyAppliedMapTo[PayloadT <: AnyRef, KeyT <: SessionKey] (
		override protected val builder : HttpRequestBuilder
		)
		(implicit override val configuration : GatlingConfiguration)
		extends LowerPriorityApply[PayloadT, KeyT]
	{
		/**
		 * This version of the apply method uses compile-type information to
		 * ensure the ''PayloadT'' can be associated with the given '''key'''.
		 */
		def apply[K <: KeyT with SessionKey.Definition[KeyT, PayloadT]] (key : K)
			(implicit decoder : Decoder[PayloadT])
			: HttpRequestBuilder =
			builder.check {
				bodyString.transformOption (
					_.fold[Validation[Option[PayloadT]]] (
						"missing JSON body".failure
						) {
						parse (_).flatMap (decoder.decodeJson)
							.map (Option (_))
							.toTry
							.toValidation
						}
					)
					.saveAs (key.entryName)
			}
	}
}

