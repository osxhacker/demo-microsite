package com.github.osxhacker.demo.chassis

import cats.data.ValidatedNec


/**
 * ==Overview==
 *
 * The '''domain''' `package` defines types which assist in the implementation
 * of microservice Domain Object Models.
 */
package object domain
{
	/// Class Types
	/**
	 * The '''ChimneyErrors''' type defines the container used to capture one
	 * or more errors detected by [[io.scalaland.chimney.TransformerF]]'s.
	 */
	type ChimneyErrors[+A] = ValidatedNec[String, A]


	/**
	 * The '''ErrorOr''' type defines the contract for [[scala.Either]] types
	 * which represent an error or an instance of ''A''.
	 */
	type ErrorOr[+A] = Either[Throwable, A]


	/**
	 * The ReaderWriterStateErrorT type defines the contract for using
	 * [[com.github.osxhacker.demo.chassis.domain.IndexedReaderWriterStateErrorT]]
	 * with the same initial and ending state types.
	 */
	type ReaderWriterStateErrorT[F[_], EnvT, LogT, ErrorT, S, A] =
		IndexedReaderWriterStateErrorT[F, EnvT, LogT, ErrorT, S, S, A]
}

