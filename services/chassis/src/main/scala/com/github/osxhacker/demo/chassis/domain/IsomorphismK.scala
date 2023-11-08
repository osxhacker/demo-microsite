package com.github.osxhacker.demo.chassis.domain

import scala.annotation.implicitNotFound
import scala.language.implicitConversions

import cats.~>


/**
 * The '''IsomorphismK''' type defines the contract for a higher-kinded
 * [[https://en.wikipedia.org/wiki/Isomorphism Isomorphism]] between the
 * containers `F[_]` and `G[_]`.
 */
final class IsomorphismK[F[_], G[_]] (
	private val toF : G ~> F,
	private val toG : F ~> G
	)
{
	/// Instance Properties
	lazy val reverse : IsomorphismK[G, F] = new IsomorphismK (toG, toF)


	/**
	 * Alias for `to`.
	 */
	def apply[A] (fa : F[A]) : G[A] = to (fa)


	/**
	 * The to method transforms '''fa''' into an instance of `G[A]`.
	 */
	def to[A] (fa : F[A]) : G[A] = toG (fa)
}


object IsomorphismK
{
	/**
	 * The apply method constructs an '''IsomorphismK''' instance based on the
	 * `implicit` natural transformations '''toG''' and '''toF'''.
	 */
	def apply[F[_], G[_]] ()
		(
			implicit

			@implicitNotFound (
				"could not resolve natural transform: ${F} ~> ${G}"
				)
			toG : F ~> G,

			@implicitNotFound (
				"could not resolve natural transform: ${G} ~> ${F}"
				 )
			toF : G ~> F
		)
		: IsomorphismK[F, G] =
		new IsomorphismK (toF, toG)


	/// Implicit Conversions
	implicit def summonIsomorphismK[F[_], G[_]] (
		implicit
		toG : F ~> G,
		toF : G ~> F
		)
		: IsomorphismK[F, G] =
		apply ()
}

