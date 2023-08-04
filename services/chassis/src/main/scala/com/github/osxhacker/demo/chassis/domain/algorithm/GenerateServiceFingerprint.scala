package com.github.osxhacker.demo.chassis.domain.algorithm

import java.security.{
	MessageDigest,
	SecureRandom
	}

import scala.util.Try

import cats.ApplicativeThrow

import com.github.osxhacker.demo.chassis.domain.event.ServiceFingerprint


/**
 * The '''GenerateServiceFingerprint''' type defines the algorithm responsible
 * for producing a [[https://en.wikipedia.org/wiki/SHA-2 SHA-256]] "fingerprint"
 * for a specific microservice instance.
 */
object GenerateServiceFingerprint
{
	/// Class Imports
	import cats.syntax.try_._


	/// Instance Properties
	private val algorithm = "SHA-256"
	private val bufferSize = 512


	/**
	 * The apply method computes a
	 * [[https://en.wikipedia.org/wiki/SHA-2 SHA-256]] hash from arbitrary
	 * random data, since deriving content to produce the hash from machine
	 * information (such as host name, process ID, etc.) does not reliably
	 * identify an individual service instance in many deployment scenarios.
	 */
	def apply[F[_]] ()
		(implicit applicativeThrow : ApplicativeThrow[F])
		: F[ServiceFingerprint] =
		Try (MessageDigest.getInstance (algorithm)).flatMap {
			digester =>
				val bytes = new Array[Byte] (bufferSize)
				val random = new SecureRandom ()

				random.nextBytes (bytes)
				ServiceFingerprint.from[Try] (digester.digest (bytes))
			}
			.liftTo[F]
}

