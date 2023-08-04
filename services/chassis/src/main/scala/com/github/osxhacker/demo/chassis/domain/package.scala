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
}

