package com.github.osxhacker.demo.chassis.effect

import scala.util.Try

import cats.{
	Eval,
	Later
	}

import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec


/**
 * The '''PointcutSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.effect.Aspect]] for fitness of purpose
 * and serves as an exemplar of its use.
 */
final class AspectSpec ()
	extends AnyWordSpec
		with Diagrams
{
	/// Class Imports
	import cats.syntax.applicative._


	/// Class Types
	final case class EachUseAspect[F[_]] (
		private val effect : StringBuilder,
		private val prefix : String,
		private val suffix : String
		)
		extends Advice[F, String]
	{
		override def apply (fa : Eval[F[String]])
			(implicit pointcut : Pointcut[F])
			: Eval[F[String]] =
			pointcut.after (fa) {
				effect.append (prefix)
					.append (_)
					.append (suffix)
				}
	}


	final case class StaticAdvice[F[_], A] (val history : StringBuilder)
		extends Advice[F, A]
	{
		override def apply (fa : Eval[F[A]])
			(implicit pointcut : Pointcut[F])
			: Eval[F[A]] =
			pointcut.around (fa) (
				entering = () => history.append ('e'),
				leaving = _ => history.append ('l'),
				onError = () => history.append ('x')
				)
	}


	object StaticAdvice
	{
		implicit def summon[F[_], A] : StaticAdvice[F, A] =
			new StaticAdvice[F, A] (new StringBuilder ())
	}


	"The Aspect type" must {
		implicit val pointcut = Pointcut[Try] ()

		"be able to create 'static' aspects" in {
			val aspect = Aspect[Try, StaticAdvice[Try, Int]].static ()
			val result = aspect (Later (42.pure[Try])).value

			assert (aspect.pointcut == pointcut)
			assert (result.isSuccess)
			assert (result.toEither.exists (_ === 42))

			aspect.advice match {
				case expected : StaticAdvice[Try, Int] =>
					assert (expected.history.toString () === "el")

				case other =>
					fail ("expected SampleAdvice, got: " + other)
				}
			}

		"be able to create 'per-call' aspects" in {
			val aspect = Aspect[Try, EachUseAspect[Try]].percall (
				(EachUseAspect[Try] (_, _, _)).tupled
				)

			val buffer = new StringBuilder ()
			val result = aspect ((buffer, "hello, ", "!")) (
				Later ("world".pure[Try])
				)
				.value

			assert (aspect.pointcut == pointcut)
			assert (result.isSuccess)
			assert (result.toEither.exists (_ === "world"))
			assert (buffer.toString () === "hello, world!")
			}
		}
}

