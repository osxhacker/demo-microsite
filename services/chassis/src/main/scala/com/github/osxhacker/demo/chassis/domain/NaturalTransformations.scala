package com.github.osxhacker.demo.chassis.domain

import scala.reflect.ClassTag

import cats.{
	~>,
	ApplicativeThrow
	}

import cats.data.{
	Kleisli,
	Validated,
	ValidatedNec
	}

import io.scalaland.chimney.TransformerF

import com.github.osxhacker.demo.chassis.domain.error.ValidationError


/**
 * The '''NaturalTransformations''' type defines [[cats.arrow.FunctionK]]
 * instances to support commonly used libraries and
 * [[com.github.osxhacker.demo.chassis]] types.
 */
trait NaturalTransformations
	extends LowPriorityNaturalTransformations
{
	/// Class Imports
	import cats.syntax.applicative._
	import cats.syntax.applicativeError._
	import cats.syntax.either._


	/// Implicit Conversions
	implicit def errorOrToF[F[_]] (
		implicit applicativeThrow : ApplicativeThrow[F]
		)
		: ErrorOr ~> F =
		new (ErrorOr ~> F) {
			override def apply[A] (fa : ErrorOr[A]) : F[A] =
				fa.liftTo[F]
			}


	implicit def function1FToKleisli[F[_], G[_], A] (
		implicit transformer : F ~> G
		)
		: Lambda[R => A => F[R]] ~> Kleisli[G, A, *] =
		new (Lambda[R => A => F[R]] ~> Kleisli[G, A, *]) {
			override def apply[B] (fa : A => F[B]) : Kleisli[G, A, B] =
				Kleisli (fa).mapK (transformer)
			}


	implicit def transformerFToKleisli[F[_], A] (
		implicit
		applicativeThrow : ApplicativeThrow[F],
		classTag : ClassTag[A]
		)
		: TransformerF[ValidatedNec[String, +*], A, *] ~> Kleisli[F, A, *] =
		new (TransformerF[ValidatedNec[String, +*], A, *] ~> Kleisli[F, A, *]) {
			override def apply[B] (
				fa : TransformerF[ValidatedNec[String, +*], A, B]
				)
				: Kleisli[F, A, B] =
				Kleisli {
					fa.transform (_)
						.leftMap (ValidationError[A] (_))
						.toEither
						.liftTo[F]
					}
			}


	implicit def validatedNecStringToF[F[_]] (
		implicit applicativeThrow : ApplicativeThrow[F]
		)
		: ValidatedNec[String, *] ~> F =
		new (ValidatedNec[String, *] ~> F) {
			override def apply[A] (fa : ValidatedNec[String, A]) : F[A] =
				fa.fold (
					ValidationError (_).raiseError[F, A],
					_.pure
					)
			}
}


sealed trait LowPriorityNaturalTransformations
	extends LowestPriorityNaturalTransformations
{
	/// Class Imports
	import cats.syntax.applicative._
	import cats.syntax.applicativeError._


	/// Implicit Conversions
	implicit def errorOrToThrowableValidatedNec
		: ErrorOr ~> ValidatedNec[Throwable, *] =
		new (ErrorOr ~> ValidatedNec[Throwable, *]) {
			override def apply[A] (fa : ErrorOr[A])
				: ValidatedNec[Throwable, A] =
				Validated.fromEither (fa)
					.toValidatedNec
			}


	implicit def kleisliIdentityNaturalTransformation[F[_], A]
		: Kleisli[F, A, *] ~> Kleisli[F, A, *] =
		new (Kleisli[F, A, *] ~> Kleisli[F, A, *]) {
			override def apply[B] (fa : Kleisli[F, A, B]) : Kleisli[F, A, B] =
				fa
			}


	implicit def validatedNecThrowableToF[F[_]] (
		implicit applicativeThrow : ApplicativeThrow[F]
		)
		: ValidatedNec[Throwable, *] ~> F =
		new (ValidatedNec[Throwable, *] ~> F) {
			override def apply[A] (fa : ValidatedNec[Throwable, A]) : F[A] =
				fa.fold (
					errors =>
						ValidationError (errors.map (_.getMessage)).raiseError,

					_.pure
					)
		}
}


sealed trait LowestPriorityNaturalTransformations
{
	/// Implicit Conversions
	implicit def idNaturalTransformation[F[_]] : F ~> F =
		new (F ~> F) {
			override def apply[A] (fa : F[A]) : F[A] = fa
			}
}

