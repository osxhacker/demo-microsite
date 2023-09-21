package com.github.osxhacker.demo.chassis.effect

import cats.{
	ApplicativeThrow,
	Eval
	}


/**
 * The '''Advice''' type defines the contract for making logic using the
 * [[https://www.artima.com/articles/scalas-stackable-trait-pattern Stackable Trait Pattern]]
 * to complete being able to employ a limited form of
 * [[https://en.wikipedia.org/wiki/Aspect-oriented_programming Aspect Oriented Programming]]
 * natively in Scala.
 *
 * ==Implementation Notes==
 *
 * In order to successfully employ the
 * [[https://www.artima.com/articles/scalas-stackable-trait-pattern Stackable Trait Pattern]],
 * the type hierarchy must have a specific form.  The `trait`s added to a
 * concrete '''Advice''' __must__ have a known `apply` definition visible to the
 * compiler when they are "stacked."  What does this mean?
 *
 * An `abstract class` with an implementation of `apply` must be the
 * __immediate ancestor__ of any concrete '''Advice'''.  For example:
 *
 * {{{
 *     trait AdviceLogic[F[_], ResultT]
 *         extends Advice[F[_], Result]
 *     {
 *         abstract override def apply (fa : Eval[F[A]])
 *             (implicit pointcut : Pointcut[F])
 *             : F[A] =
 *             orthogonalLogicWith (super.apply (fa))
 *     }
 *
 *
 *     sealed abstract class CustomApplyAdvice[F[_], ResultT] ()
 *         extends Advice[F[_], ResultT]
 *     {
 *         override def apply (fa : Eval[F[A]])
 *             (implicit pointcut : Pointcut[F])
 *             : F[A] =
 *             maybeDoSomethingAndReturn (fa)
 *     }
 *
 *
 *     final case class MyAdvice[F[_], ResultT] ()
 *         extends CustomApplyAdvice[F[_], ResultT]
 *             with AdviceLogic[F[_], ResultT]
 * }}}
 *
 * This pattern ensures that the `apply` method in ''CustomApplyAdvice'' is the
 * __last__ (or "innermost") to execute and thus the `fa` instance __it__
 * returns is the one each stacked advice will receive.
 *
 * @see [[com.github.osxhacker.demo.chassis.effect.Pointcut]]
 */
trait Advice[F[_], ResultT]
{
	/// Instance Properties
	implicit protected def applicativeThrow : ApplicativeThrow[F]


	def apply (fa : Eval[F[ResultT]])
		(implicit pointcut : Pointcut[F])
		: Eval[F[ResultT]]
}

