package com.github.osxhacker.demo.chassis.monitoring

import java.util.UUID

import scala.util.Try

import cats.{
	ApplicativeThrow,
	Eq,
	Show
	}


/**
 * The '''CorrelationId''' type reifies the concept of a [[java.util.UUID]]
 * compatible value which represents the initiation of service functionality
 * originating from an external agent.  Here, "external agent" can be defined as
 * software which exists outside the boundaries of deployed system assets.  For
 * example, an HTTP request from a web browser or an administrative utility
 * program.
 */
final case class CorrelationId (private val value : UUID)
{
	/**
	 * The toUuid method produces a [[java.util.UUID]] from '''this''' instance.
	 * How '''this''' becomes a [[java.util.UUID]] is implementation-defined.
	 * This method will __always__ succeed assuming sufficient memory is
	 * available.
	 */
	def toUuid () : UUID = value
}


object CorrelationId
{
	/// Class Imports
	import cats.syntax.either._
	import mouse.boolean._


	/// Instance Properties
	val MinimumUuidVersion : 3 = 3
	val UnsupportedUuidVersion = new IllegalArgumentException (
		"unsupported UUID version detected"
		)


	/**
	 * This version of the apply method is provided to support functional-style
	 * creation from an arbitrary '''candidate'''.  If the ''String'' does
	 * __not__ a representation of a
	 * [[https://en.wikipedia.org/wiki/Universally_unique_identifier v3+]]
	 * instance, then this operation will fail.
	 */
	def apply[F[_]] (candidate : String)
		(implicit applicativeThrow : ApplicativeThrow[F])
		: F[CorrelationId] =
		Try (UUID.fromString (candidate))
			.toEither
			.filterOrElse (
				_.version () >= MinimumUuidVersion,
				UnsupportedUuidVersion
				)
			.map (new CorrelationId (_))
			.liftTo[F]


	/**
	 * This version of the apply method is provided to support functional-style
	 * creation from a given '''uuid'''.  If the [[java.util.UUID]] is __not__
	 * a [[https://en.wikipedia.org/wiki/Universally_unique_identifier v3+]]
	 * instance, then this operation will fail.
	 */
	def apply[F[_]] (uuid : UUID)
		(implicit applicativeThrow : ApplicativeThrow[F])
		: F[CorrelationId] =
		Either.cond (
			uuid.version () >= MinimumUuidVersion,
			new CorrelationId (uuid),
			UnsupportedUuidVersion
			)
			.liftTo[F]


	/// Implicit Conversions
	implicit val correlationIdEq : Eq[CorrelationId] = Eq.fromUniversalEquals
	implicit val correlationIdShow : Show[CorrelationId] = Show.show (
		_.value.toString
		)
}

