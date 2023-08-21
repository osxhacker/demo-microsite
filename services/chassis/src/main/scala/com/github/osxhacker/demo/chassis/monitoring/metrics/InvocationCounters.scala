package com.github.osxhacker.demo.chassis.monitoring.metrics

import cats.Eval
import kamon.Kamon

import com.github.osxhacker.demo.chassis.effect.{
	Advice,
	Pointcut
	}


/**
 * The '''InvocationCounters''' type defines an
 * [[com.github.osxhacker.demo.chassis.effect.Advice]] which tracks how many
 * times the production of an ''F[ResultT]'' instance is made, how many times
 * it fails, and how many times it succeeds.  Note that a successful invocation
 * __may__ have a ''ResultT'' which represents a problem for its domain.
 */
trait InvocationCounters[F[_], ResultT]
	extends Advice[F, ResultT]
{
	/// Self Type Constraints
	this : MetricsAdvice[F, ResultT] =>


	/// Instance Properties
	private[metrics] lazy val called =
		Kamon.counter (mkName ("called"))
			.withTags (tags)

	private[metrics] lazy val failed =
		Kamon.counter (mkName ("failed"))
			.withTags (tags)

	private[metrics] lazy val succeeded =
		Kamon.counter (mkName ("succeeded"))
			.withTags (tags)


	abstract override def apply (fa : Eval[F[ResultT]])
		(implicit pointcut : Pointcut[F])
		: Eval[F[ResultT]] =
		pointcut.around (super.apply (fa)) (
			entering = () => called.increment (),
			leaving = _ => succeeded.increment (),
			onError = _ => failed.increment ()
			)
}

