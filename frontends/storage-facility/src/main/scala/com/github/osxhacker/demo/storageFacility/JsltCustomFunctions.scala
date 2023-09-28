package com.github.osxhacker.demo.storageFacility

import java.net.{
	URLDecoder,
	URLEncoder
	}

import java.util.{
	Arrays,
	Collection => JCollection
	}

import scala.util.Sorting

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{
	ArrayNode,
	TextNode
	}

import com.schibsted.spt.data.jslt.{
	Function => JsltFunction
	}

import org.apache.camel.{
	BindToRegistry,
	Configuration
	}


/**
 * The '''JsltCustomFunctions''' type defines configuration for the
 * [[https://camel.apache.org/components/3.20.x/jslt-component.html Camel JSLT]]
 * component.  It would be defined in
 * [[https://camel.apache.org/components/3.20.x/others/main.html Camel Main]] if
 * there was a way to create the a ''Array[Class]'' parameter for use in
 * `wrapStaticMethod` within XML/properties configuration.
 */
@Configuration
final class JsltCustomFunctions ()
{
	/**
	 * The all method creates the
	 * [[https://github.com/schibsted/jslt/blob/master/extensions.md extension]]
	 * methods available to every
	 * [[https://github.com/schibsted/jslt JSLT]] Camel route.
	 */
	@BindToRegistry ("jslt-custom-functions")
	def all () : JCollection[JsltFunction] =
		Arrays.asList[JsltFunction] (
			JsltCustomFunctions.ArraySortBy,
			JsltCustomFunctions.DecodeString,
			JsltCustomFunctions.EncodeString
			)
}


object JsltCustomFunctions
{
	/// Class Types
	/**
	 * The '''ArraySortBy''' `object` defines the custom
	 * [[com.schibsted.spt.data.jslt.Function]] responsible for providing the
	 * ability to __lexicographically__ sort a JSON array of objects using a
	 * given [[https://www.rfc-editor.org/rfc/rfc6901 JSON Pointer]] to identify
	 * what "key" to use.
	 *
	 * There are two ways `sort-by` can be used.  First is the two argument
	 * form:
	 *
	 * {{{
	 *     { "foo" : sort-by ("/some/key", .an.array) }
	 * }}}
	 *
	 * The second way is to use the `|` operator and specify only the
	 * [[https://www.rfc-editor.org/rfc/rfc6901 JSON Pointer]]:
	 *
	 * {{{
	 *     { "foo" : [ ... ] | sort-by ("/some/key") }
	 * }}}
	 */
	private object ArraySortBy
		extends JsltFunction
	{
		/// Class Types
		private final case class NodeOrdering (private val keyLocation : String)
			extends Ordering[JsonNode]
		{
			override def compare (a : JsonNode, b : JsonNode) : Int =
				keyFor (a).zip (keyFor (b))
					.map {
						case (ka, kb) =>
							ka.compareTo (kb)
						}
					.getOrElse (0)


			@inline
			private def keyFor (root : JsonNode) : Option[String] =
				Option (root.at (keyLocation)).filterNot (_.isNull)
					.map (_.asText ())
		}


		/// Instance Properties
		override val getName : String = "sort-by"
		override val getMaxArguments : Int = 2
		override val getMinArguments : Int = 1


		override def call (input : JsonNode, arguments : Array[JsonNode])
			: JsonNode =
		{
			val (keyPath, unsortedArray) = pickArguments (input, arguments)

			arrayOfAllObjects (unsortedArray).map {
				arrayNode =>
					implicit val ordering = NodeOrdering (
						keyPath.asText ()
						)

					val array = new Array[JsonNode] (arrayNode.size ())

					for (n <- 0 until arrayNode.size ())
						array (n) = arrayNode.get (n)

					Sorting.stableSort (array)

					for (n <- 0 until arrayNode.size ())
						arrayNode.set (n, array (n))

					arrayNode
				}
				.getOrElse (unsortedArray)
		}


		@inline
		private def arrayOfAllObjects (node : JsonNode) : Option[ArrayNode] =
			Option.when (node.isArray) (node.asInstanceOf[ArrayNode])
				.filterNot (_.isEmpty)
				.filter {
					array =>
						(0 until array.size ()).forall {
							array.get (_).isObject
							}
					}


		@inline
		private def pickArguments (input : JsonNode, params : Array[JsonNode])
			: (JsonNode, JsonNode) =
			if (params.length == getMaxArguments)
				(params (0), params (1))
			else
				(params (0), input)
	}


	/**
	 * The '''DecodeString''' `object` defines the
	 * [[com.schibsted.spt.data.jslt.Function]] responsible for providing
	 * [[java.net.URLDecoder]] as a [[https://github.com/schibsted/jslt JSLT]]
	 * function.
	 */
	private object DecodeString
		extends JsltFunction
	{
		/// Instance Properties
		override val getName : String = "url-decode"
		override val getMinArguments : Int = 1
		override val getMaxArguments : Int = 1


		override def call (input : JsonNode, arguments : Array[JsonNode])
			: JsonNode =
			new TextNode (URLDecoder.decode (arguments (0).asText (), utf8))
	}


	/**
	 * The '''EncodeString''' `object` defines the
	 * [[com.schibsted.spt.data.jslt.Function]] responsible for providing
	 * [[java.net.URLEncoder]] as a [[https://github.com/schibsted/jslt JSLT]]
	 * function.
	 */
	private object EncodeString
		extends JsltFunction
	{
		/// Instance Properties
		override val getName : String = "url-encode"
		override val getMinArguments : Int = 1
		override val getMaxArguments : Int = 1


		override def call (input : JsonNode, arguments : Array[JsonNode])
			: JsonNode =
			new TextNode (URLEncoder.encode (arguments (0).asText (), utf8))
	}


	/// Instance Properties
	private val utf8 = "UTF-8"
}

