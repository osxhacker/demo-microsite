package com.github.osxhacker.demo.chassis.monitoring.metrics

import scala.language.implicitConversions

import cats.Later

import com.github.osxhacker.demo.chassis.effect._


/**
 * The '''MetricsSyntax''' type provides syntactic sugar for using
 * [[com.github.osxhacker.demo.chassis.monitoring.metrics.MetricsAdvice]]-based
 * [[com.github.osxhacker.demo.chassis.effect.StaticAspect]]s and/or
 * [[com.github.osxhacker.demo.chassis.effect.PercallAspect]]s within a
 * supported container ''F''.
 */
trait MetricsSyntax
{
	/// Implicit Conversions
	implicit final def metricsOps[F[_], A] (fa : => F[A]) : MetricsOps[F, A] =
		new MetricsOps[F, A] (Later (fa))
}


final class MetricsOps[F[_], A] (private val self : Later[F[A]])
	extends AnyVal
{
	/**
	 * @see [[com.github.osxhacker.demo.chassis.effect.StaticAspect]]
	 */
	def measure[AdviceT <: Advice[F, A]] ()
		(
			implicit
			theAdvice : AdviceT,
			thePointcut : Pointcut[F]
		)
		: F[A] =
		StaticAspect[F, AdviceT, A] (theAdvice).apply (self)
			.value


	/**
	 * @see [[com.github.osxhacker.demo.chassis.effect.Aspect]]
	 */
	def measure[AdviceT <: Advice[F, A]] (advice : => AdviceT)
		(
			implicit
			ev : AdviceT <:< Advice[F, A],
			pointcut : Pointcut[F]
		)
		: F[A] =
		Aspect[F, AdviceT].static[A] (advice).apply (self)
			.value


	/**
	 * @see [[com.github.osxhacker.demo.chassis.effect.Aspect]]
	 */
	def measureEachTime[AdviceT <: Advice[F, A]] (advice : => AdviceT)
		(implicit pointcut : Pointcut[F])
		: F[A] =
		Aspect[F, AdviceT].percall[Unit] (_ => advice)
			.apply ({}) (self)
			.value
}

