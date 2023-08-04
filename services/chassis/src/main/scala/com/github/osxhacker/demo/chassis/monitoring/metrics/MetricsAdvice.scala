package com.github.osxhacker.demo.chassis.monitoring.metrics

import cats.data.NonEmptyList
import kamon.context.Storage.Scope
import kamon.tag.TagSet
import kamon.trace.Span

import com.github.osxhacker.demo.chassis.effect.Advice


/**
 * The '''MetricsAdvice''' type defines the common functionality for __all__
 * [[com.github.osxhacker.demo.chassis.effect.Advice]] relating to collecting
 * system metrics.
 *
 * @see [[https://kamon.io/docs/latest/core/metrics/]]
 */
trait MetricsAdvice[F[_], ResultT]
{
	/// Self Type Constraints
	this : Advice[F, ResultT] =>


	/// Class Imports
	import cats.syntax.foldable._
	import mouse.any._


	/// Instance Properties
	def group : NonEmptyList[String] = NonEmptyList.of[String] (
		"app",
		"metrics"
		)


	def tags : TagSet = TagSet.Empty


	/**
	 * This version of the mkName method creates a ''String'' containing the
	 * "dotted parts" from the concrete type's '''group'''.
	 */
	protected def mkName () : String = mkName (group)


	/**
	 * This version of the mkName method creates a ''String'' containing the
	 * "dotted parts" from the concrete type's '''group''' and an additional
	 * '''leaf''' component.
	 */
	protected def mkName (leaf : String) : String =
		subgroup (group, leaf) |> mkName


	/**
	 * This version of the mkName method creates a ''String'' containing the
	 * "dotted parts" from the given '''parts'''.
	 */
	protected def mkName (parts : NonEmptyList[String]) : String =
		parts.mkString_ (".")


	/**
	 * The subgroup method creates a new [[cats.data.NonEmptyList]] having the
	 * '''name''' and any '''additional''' parts appended to the given
	 * '''parent'''.
	 */
	protected def subgroup (
		parent : NonEmptyList[String],
		name : String,
		additional : String *
		)
		: NonEmptyList[String] =
		parent ::: NonEmptyList.of[String] (name, additional :_*)
}


object MetricsAdvice
{
	/// Class Types
	/**
	 * The '''ManagedSpan''' type is an internal management `class`
	 * responsible for ensuring that the `scope` and `span` are properly
	 * informed as to when they are no longer needed and in requisite order.
	 */
	final case class ManagedSpan private (
		private val span : Span,
		private val scope : Scope
		)
	{
		def cleanup () : Unit =
		{
			scope.close ()
			span.finish ()
		}


		def failWith (message : String) : Unit =
		{
			scope.close ()
			span.fail (message)
				.finish ()
		}


		def failWith (error : Throwable) : Unit =
		{
			scope.close ()
			span.fail (error)
				.finish ()
		}
	}
}

