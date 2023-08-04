package com.github.osxhacker.demo.chassis.effect

import cats.Eval


/**
 * The '''DefaultAdvice''' type provides a default `apply` implementation for
 * the [[com.github.osxhacker.demo.chassis.effect.Advice]] type and can be used
 * in the manifestation of the
 * [[https://www.artima.com/articles/scalas-stackable-trait-pattern Stackable Trait Pattern]].
 */
class DefaultAdvice[F[_], ResultT] ()
	extends Advice[F, ResultT]
{
	override def apply (fa : Eval[F[ResultT]])
		(implicit pointcut : Pointcut[F])
		: Eval[F[ResultT]] =
		fa
}

