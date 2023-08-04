package com.github.osxhacker.demo.chassis.adapter.rest.arrow

import java.net.URI

import scala.language.postfixOps

import cats.arrow.{
	Arrow,
	FunctionK
	}

import eu.timepit.refined
import shapeless.tag.@@
import sttp.model.{
	MediaType,
	Method
	}

import com.github.osxhacker.demo.chassis.adapter.rest._
import com.github.osxhacker.demo.chassis.domain.entity.Identifier


/**
 * The '''AbstractToApi''' type defines the workflow to produce an ''ApiT'' from
 * a ''DomainT'' and minimal collaborators.
 *
 * The steps employed are, in order:
 *
 *   - Transform a ''DomainT'' instance into a ''F[ApiT]''.
 *
 *   - Optionally expand embedded objects in the ''ApiT''.
 *
 *   - Add semantic links to the ''ApiT''.
 */
abstract class AbstractToApi[ArrowT[_, _], F[+_], DomainT, ApiT] ()
	(
		implicit
		/// Needed for '>>>', `first`, and `lift`.
		private val arrow : Arrow[ArrowT],

		/// Needed to transform `factory` into an `ArrowT`.
		private val transformerToArrowT : FunctionK[
			Lambda[A => DomainT => F[A]],
			ArrowT[DomainT, *]
			]
	)
{
	/// Class Imports
	import cats.syntax.arrow._
	import cats.syntax.compose._
	import cats.syntax.profunctor._
	import cats.syntax.strong._


	/// Class Types
	final type ResultType = ArrowT[(DomainT, ResourceLocation @@ ApiT), ApiT]


	/// Instance Properties
	protected def addLinks : ArrowT[(DomainT, ApiT, ResourceLocation @@ ApiT), ApiT]
	protected def factory : DomainT => F[ApiT]


	/**
	 * The apply method is an alias for `run`.
	 */
	final def apply () : ResultType = run ()


	/**
	 * The apply method is an alias for `run`.
	 */
	final def apply (expander : ArrowT[(DomainT, ApiT), ApiT]) : ResultType =
		run (expander)


	/**
	 * This version of the run method produces an ''ArrowT'' which __does not__
	 * support reference expansion and has __all__ steps needed to produce an
	 * ''ApiT'' from an arbitrary ''DomainT'' along with a tagged
	 * [[com.github.osxhacker.demo.chassis.adapter.rest.ResourceLocation]].
	 */
	final def run () : ResultType = run (arrow.lift (_._2))


	/**
	 * This version of the run method produces an ''ArrowT'' which __supports__
	 * reference expansion and has __all__ steps needed to produce an ''ApiT''
	 * from an arbitrary ''DomainT'' along with a tagged
	 * [[com.github.osxhacker.demo.chassis.adapter.rest.ResourceLocation]].
	 */
	final def run (expander : ArrowT[(DomainT, ApiT), ApiT]) : ResultType =
		(transformerToArrowT (factory).second[DomainT] >>> expander)
			.lmap[DomainT] (entity => entity -> entity)
			.first[ResourceLocation @@ ApiT]
			.merge (arrow.id) >>>
			arrow.lift {
				case ((resource, location), (entity, _)) =>
					(entity, resource, location)
				} >>>
			addLinks
}


object AbstractToApi
{
	/// Class Types
	/**
	 * The '''Companion''' type defines types and functionality useful in
	 * constructing `api` ''ResourceObject'' definitions.
	 */
	abstract class Companion[EntityT] ()
		(implicit private val namespace : Identifier.EntityNamespace[EntityT])
	{
		/// Class Imports
		import refined.api.Refined
		import refined.boolean.And
		import refined.collection.NonEmpty
		import refined.string.Trimmed


		/// Class Types
		final type CommandNameType = Refined[
			String,
			Trimmed And NonEmpty
			]


		/**
		 * The '''ChangeStatus''' type defines the
		 * [[com.github.osxhacker.demo.chassis.adapter.rest.SemanticLink]] which
		 * is associated with changing the status of a Domain Object Model
		 * entity.
		 */
		final protected case class ChangeStatus (
			override val href : URI,
			override val rel : Relation
			)
			extends SemanticLink (
				method = Method.POST,
				href = href,
				rel = rel,
				mediaType = MediaType.ApplicationJson
				)


		/**
		 * The commandUrn method creates a
		 * [[https://en.wikipedia.org/wiki/Uniform_Resource_Name URN]]
		 * [[java.net.URI]] composed of the `implicit` '''namespace''' for an
		 * arbitrary ''EntityT'' and the given '''command''', which constitutes
		 * the "nss" portion of the
		 * [[https://en.wikipedia.org/wiki/Uniform_Resource_Name URN]].
		 */
		protected def commandUrn (command : CommandNameType)
			: URI =
			new URI (
				new StringBuilder ()
					.append ("urn:")
					.append (namespace ().value)
					.append (':')
					.append (command.value)
					.toString ()
				)
	}
}
