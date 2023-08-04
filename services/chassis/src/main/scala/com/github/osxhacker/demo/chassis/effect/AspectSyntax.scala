package com.github.osxhacker.demo.chassis.effect

import scala.language.implicitConversions

import cats.Later


/**
 * The '''AspectSyntax''' type provides syntactic sugar for using
 * [[com.github.osxhacker.demo.chassis.effect.StaticAspect]]s and/or
 * [[com.github.osxhacker.demo.chassis.effect.PercallAspect]]s within a
 * supported container ''F''.
 */
trait AspectSyntax
{
	/// Implicit Conversions
	implicit def aspectOps[F[_], A] (fa : => F[A]) : AspectOps[F, A] =
		new AspectOps[F, A] (Later (fa))
}


final class AspectOps[F[_], A] (private val self : Later[F[A]])
	extends AnyVal
{
	/**
	 * @see [[com.github.osxhacker.demo.chassis.effect.Aspect]]
	 */
	def percall[AdviceT <: Advice[F, A]] (advice : => AdviceT)
		(implicit pointcut : Pointcut[F])
		: F[A] =
		Aspect[F, AdviceT].percall[Unit] (_ => advice)
			.apply ({}) (self)
			.value


	/**
	 * @see [[com.github.osxhacker.demo.chassis.effect.Aspect]]
	 */
	def static[AdviceT <: Advice[F, A]] ()
		(implicit staticAspect : StaticAspect.Aux[F, AdviceT, A])
		: F[A] =
		Aspect[F, AdviceT].static[A]().apply (self)
			.value


	/**
	 * @see [[com.github.osxhacker.demo.chassis.effect.Aspect]]
	 */
	def static[AdviceT <: Advice[F, A]] (advice : => AdviceT)
		(
			implicit
			ev : AdviceT <:< Advice[F, A],
			pointcut : Pointcut[F]
		)
		: F[A] =
		Aspect[F, AdviceT].static (advice).apply (self)
			.value
}

