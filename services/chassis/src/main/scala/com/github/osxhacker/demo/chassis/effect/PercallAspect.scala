package com.github.osxhacker.demo.chassis.effect

import cats.Eval


/**
 * The '''PercallAspect''' type defines an
 * [[https://en.wikipedia.org/wiki/Aspect-oriented_programming Aspect Oriented Programming]]
 * aspect which creates a new ''AdviceT'' instance each time `apply` is called.
 */
final case class PercallAspect[F[_], AdviceT <: Advice[F, _], ContextT] (
	private val factory : ContextT => AdviceT
	)
	(implicit private[effect] val pointcut : Pointcut[F])
{
	def apply[A] (context : ContextT)
		(fa : Eval[F[A]])
		(implicit ev : AdviceT <:< Advice[F, A])
		: Eval[F[A]] =
		ev (factory (context)) (fa)
}

