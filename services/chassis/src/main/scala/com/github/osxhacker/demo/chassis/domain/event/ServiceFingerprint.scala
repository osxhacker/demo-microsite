package com.github.osxhacker.demo.chassis.domain.event

import cats.{
	ApplicativeThrow,
	Eq,
	Show
	}

import eu.timepit.refined.types.digests.SHA256
import monocle.Getter

import com.github.osxhacker.demo.chassis.domain.error.LogicError


/**
 * The '''ServiceFingerprint''' type defines the Domain Object Model concept of
 * a unique microservice signature.  In this context, uniqueness is only
 * guaranteed for the duration of a microservice process and can differ between
 * two invocations of the same microservice.
 */
final case class ServiceFingerprint (private val value : SHA256)
{
	override def toString () : String = value.value
}


object ServiceFingerprint
{
	/// Class Imports
	import cats.syntax.either._


	/// Instance Properties
	val value = Getter[ServiceFingerprint, SHA256] (_.value)


	/**
	 * This version of the from method attempts to create a
	 * '''ServiceFingerprint''' from a given '''digest''' ''Array[Byte]'',
	 * failing if the '''digest''' does not represent a valid
	 * [[https://en.wikipedia.org/wiki/SHA-2 SHA-256]] value.
	 */
	def from[F[_]] (digest : Array[Byte])
		(implicit applicativeThrow : ApplicativeThrow[F])
		: F[ServiceFingerprint] =
		SHA256.from (
			digest.foldLeft (new StringBuilder ()) {
				case (accum, byte) =>
					accum.append ("%02x" format byte)
				}
			.toString ()
			)
			.bimap (
				LogicError (_),
				ServiceFingerprint (_)
				)
			.liftTo[F]


	/**
	 * This version of the from method attempts to create a
	 * '''ServiceFingerprint''' from a given '''digest''' ''String'', failing
	 * if the '''digest''' does not represent a valid
	 * [[https://en.wikipedia.org/wiki/SHA-2 SHA-256]] value.
	 */
	def from[F[_]] (digest : String)
		(implicit applicativeThrow : ApplicativeThrow[F])
		: F[ServiceFingerprint] =
		SHA256.from (digest)
			.bimap (
				LogicError (_),
				ServiceFingerprint (_)
				)
			.liftTo[F]

	/// Implicit Conversions
	implicit val serviceFingerprintEq : Eq[ServiceFingerprint] =
		Eq.by (_.value.value)

	implicit val serviceFingerprintShow : Show[ServiceFingerprint] =
		Show.show (_.value.value)
}

