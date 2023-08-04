package com.github.osxhacker.demo.chassis.monitoring.logging

import cats.Functor
import org.typelevel.log4cats.{
	LoggerFactory,
	SelfAwareStructuredLogger
	}


/**
 * The '''ContextualLoggerFactory''' type defines a
 * [[org.typelevel.log4cats.LoggerFactory]] which ensures there is always a
 * `minimumContext` associated with __each__
 * [[org.typelevel.log4cats.SelfAwareStructuredLogger]] created by the
 * `underlying` [[org.typelevel.log4cats.LoggerFactory]].
 */
final class ContextualLoggerFactory[F[_]] (
	private val underlying : LoggerFactory[F],
	private val minimumContext : Map[String, String]
	)
	(implicit functor : Functor[F])
	extends LoggerFactory[F]
{
	/// Class Imports
	import cats.syntax.functor._


	override def getLoggerFromName (name : String)
		: SelfAwareStructuredLogger[F] =
		underlying.getLoggerFromName (name)
			.addContext (minimumContext)


	override def fromName (name : String) : F[SelfAwareStructuredLogger[F]] =
		underlying.fromName (name)
			.map (_.addContext (minimumContext))


	/**
	 * The addContext methdo augments the existing `minimumContext` with the
	 * given '''properties'''.
	 */
	def addContext (properties : Map[String, String])
		: ContextualLoggerFactory[F] =
		new ContextualLoggerFactory[F] (
			underlying,
			minimumContext ++ properties
			)
}


object ContextualLoggerFactory
{
	/**
	 * The apply method is provided to support functional-style creation of
	 * '''ContextualLoggerFactory''' instances.
	 */
	def apply[F[_]] (underlying : LoggerFactory[F])
		(context : Map[String, String])
		(implicit functor : Functor[F])
		: ContextualLoggerFactory[F] =
		new ContextualLoggerFactory[F] (underlying, context)
}

