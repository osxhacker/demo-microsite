package com.github.osxhacker.demo.chassis.adapter.rest

import java.net.URI
import java.util.Objects

import io.circe.Json
import sttp.model.{
	MediaType,
	Method
	}


/**
 * The '''SemanticLink''' type reifies the concept of a
 * [[https://www.rfc-editor.org/rfc/rfc5988 Web Linking]] for RESTful services.
 * Note that the "wire format" is different than the property names defined here
 * such that `type` becomes `mediaType`.
 *
 * Equality is determined by both the '''rel''' and '''mediaType''' properties.
 */
class SemanticLink (
	val method : Method,
	val href : URI,
	val rel : Relation,
	val mediaType : MediaType,
	val title : Option[String] = None,
	val length : Option[Long] = None
	)
	extends Product6[
		Method,
		URI,
		Relation,
		MediaType,
		Option[String],
		Option[Long]
		]
{
	/// Class Imports
	import SemanticLink.names


	/// Instance Properties
	final override val _1 = method
	final override val _2 = href
	final override val _3 = rel
	final override val _4 = mediaType
	final override val _5 = title
	final override val _6 = length
	final override val productPrefix : String = "SemanticLink"


	override def canEqual (that : Any) : Boolean =
		that.isInstanceOf[SemanticLink]


	override def equals (that : Any) : Boolean =
		canEqual (that) && {
			val other = that.asInstanceOf[SemanticLink]

			rel.equals (other.rel) && mediaType.equals (other.mediaType)
			}


	override def hashCode () : Int = Objects.hash (rel, mediaType)


	override def productElementName (n : Int) : String =
		names.applyOrElse (n, index => super.productElementName (index))


	/**
	 * The toMap method creates a ''Map[String, Json]'' representation of
	 * '''this''' instance.
	 */
	final def toMap () : Map[String, Json] =
		Map (
			rel._1 -> Json.fromFields (
				List (
					"method" -> Json.fromString (method.method),
					"href" -> Json.fromString (href.toString ()),
					"rel" -> Json.fromString (rel._1),
					"mediaType" -> Json.fromString (mediaType.toString ()),
					) :::
				title.map ("title" -> Json.fromString (_)).toList :::
				length.map ("length" -> Json.fromLong (_)).toList
				)
			)
}


object SemanticLink
{
	/// Class Types
	final case class Delete (override val href : URI)
		extends SemanticLink (
			method = Method.DELETE,
			href = href,
			rel = Relation.Delete,
			mediaType = MediaType.TextPlain
			)


	final case class Edit (
		override val href : URI,
		override val mediaType : MediaType = MediaType.ApplicationJson,
		override val title : Option[String] = None
		)
		extends SemanticLink (
			method = Method.POST,
			href = href,
			rel = Relation.Edit,
			mediaType = mediaType,
			title = title
			)


	final case class Self (
		override val href : URI,
		override val mediaType : MediaType = MediaType.ApplicationJson,
		override val title : Option[String] = None,
		override val length : Option[Long] = None
		)
		extends SemanticLink (
			method = Method.GET,
			href = href,
			rel = Relation.Self,
			mediaType = mediaType,
			title = title,
			length = length
			)


	/// Instance Properties
	private val names = Seq (
		"method",
		"href",
		"rel",
		"mediaType",
		"title",
		"length"
		)


	/**
	 * The apply method is provided to support functional-style creation of
	 * '''SemanticLink''' instances.
	 */
	def apply (
		method : Method,
		href : URI,
		rel : Relation,
		mediaType : MediaType,
		title : Option[String] = None,
		length : Option[Long] = None
		)
		: SemanticLink =
		new SemanticLink (method, href, rel, mediaType, title, length)
}

