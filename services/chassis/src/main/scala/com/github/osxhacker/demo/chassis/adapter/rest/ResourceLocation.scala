package com.github.osxhacker.demo.chassis.adapter.rest

import java.net.URI

import scala.annotation.unused

import cats.{
	Eq,
	Show
	}

import sttp.tapir._
import sttp.tapir.server.ServerEndpoint

import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.chassis.domain.error.LogicError


/**
 * The '''ResourceLocation''' type reifies the concept of a
 * [[https://developer.mozilla.org/en-US/docs/web/http/headers/location HTTP Location]].
 */
final case class ResourceLocation (private val path : Path)
{
	/// Class Imports
	import cats.syntax.semigroup._


	/**
	 * The / method is an alias for `append`.
	 */
	def / (subpath : Path) : ResourceLocation = append (subpath)


	/**
	 * The append method appends a '''subpath''' to '''this''' instance.
	 */
	def append (subpath : Path) : ResourceLocation =
		copy (path |+| subpath)


	/**
	 * This version of the toUri method creates an [[java.net.URI]] with only
	 * a '''path''' component.
	 */
	def toUri () : URI = path.toUri ()
}


object ResourceLocation
{
	/// Class Imports
	import cats.syntax.either._
	import cats.syntax.show._
	import mouse.boolean._


	/**
	 * The '''Factory''' type is a model of the FACTORY pattern and provides the
	 * ability to create __fully resolved__
	 * [[com.github.osxhacker.demo.chassis.adapter.rest.ResourceLocation]]s,
	 * erroring if either __all__ `endpoint` parameters
	 */
	final class Factory[-R, F[_]] (private val endpoint : ServerEndpoint[R, F])
	{
		/// Instance Properties
		private val unresolved = "<<missing>>"


		def apply[ParamsT <: Product] (params : ParamsT)
			(implicit parser : Path.Parser[String])
			: ErrorOr[ResourceLocation] =
		{
			val rawPath = endpoint.showPathTemplate (
				showPathParam = resolveParameter (
					params.productElementNames
						.zip (params.productIterator.map (_.toString ()))
						.toMap
					)
				)

			rawPath.contains (unresolved)
				.fold (
					LogicError (
						"failed to resolve all resource location parameters"
						).asLeft,

					parser (rawPath).map (new ResourceLocation (_))
					)
		}


		private def resolveParameter (context : Map[String, String])
			(@unused index : Int, pc : EndpointInput.PathCapture[_])
			: String =
			pc.name
				.flatMap (context.get)
				.getOrElse (unresolved)
	}


	/**
	 * The template method creates a
	 * [[com.github.osxhacker.demo.chassis.adapter.rest.ResourceLocation.Factory]]
	 * capable of creating a fully resolved
	 * [[com.github.osxhacker.demo.chassis.adapter.rest.ResourceLocation]] or
	 * error if unable to do so.
	 */
	def template[R, F[_]] (endpoint : ServerEndpoint[R, F]) : Factory[R, F] =
		new Factory[R, F] (endpoint)


	private def decode (candidate : String) : DecodeResult[ResourceLocation] =
		Path.from[ErrorOr, String] (candidate)
			.fold (
				DecodeResult.Error (candidate, _),
				path => DecodeResult.Value (ResourceLocation (path))
				)


	private def encode (instance : ResourceLocation) : String =
		instance.path.show


	/// Implicit Conversions
	implicit lazy val asHeader : EndpointIO.Header[ResourceLocation] =
		sttp.tapir.header[ResourceLocation]("Location")

	implicit val codec : Codec.PlainCodec[ResourceLocation] =
		Codec.string.mapDecode (decode) (encode)


	/**
	 * While '''ResourceLocation''' is a `final case class`, when an instance
	 * is tagged using [[shapeless.tag]], invariant type classes such as
	 * [[cats.Eq]] must be parameterized.
	 */
	implicit def resourceLocationEq[A <: ResourceLocation] : Eq[A] =
		Eq.by (_.path)


	/**
	 * While '''ResourceLocation''' is a `final case class`, when an instance
	 * is tagged using [[shapeless.tag]], invariant type classes such as
	 * [[cats.Show]] must be parameterized.
	 */
	implicit def resourceLocationShow[A <: ResourceLocation] : Show[A] =
		Show.show (_.path.show)
}

