package com.github.osxhacker.demo.chassis.effect

import scala.language.{
	implicitConversions,
	postfixOps
	}

import cats.Eval


/**
 * The '''StaticAspect''' type defines an
 * [[https://en.wikipedia.org/wiki/Aspect-oriented_programming Aspect Oriented Programming]]
 * aspect which is statically bound to an
 * [[com.github.osxhacker.demo.chassis.effect.Advice]] invoked with the
 * [[com.github.osxhacker.demo.chassis.effect.Pointcut]] context's ''F''.  It is
 * important to remember that only __one__ ''AdviceT'' will be created for the
 * lifetime of a ''StaticAspect''.
 */
sealed trait StaticAspect[F[_], AdviceT <: Advice[F, _]]
{
	/// Class Types
	type ResultType


	/// Instance Properties
	implicit val pointcut : Pointcut[F]
	val advice : Advice[F, ResultType]


	def apply (fa : Eval[F[ResultType]]) : Eval[F[ResultType]] = advice (fa)
}


object StaticAspect
{
	/// Class Types
	type Aux[F[_], AdviceT <: Advice[F, _], R] = StaticAspect[F, AdviceT] {
		type ResultType = R
		}


	/**
	 * The apply method creates a '''StaticAspect''' using '''theAdvice'''
	 * and '''thePointcut''' given.
	 */
	def apply[F[_], AdviceT <: Advice[F, _], ResultT] (theAdvice : AdviceT)
		(
			implicit
			ev : AdviceT <:< Advice[F, ResultT],
			thePointcut : Pointcut[F]
		)
		: StaticAspect.Aux[F, AdviceT, ResultT] =
		new StaticAspect[F, AdviceT] {
			override type ResultType = ResultT

			override implicit val advice : Advice[F, ResultType] = theAdvice
			override implicit val pointcut : Pointcut[F] = thePointcut
			}


	/// Implicit Conversions
	implicit def summon[F[_], AdviceT <: Advice[F, _], ResultT] (
		implicit
		theAdvice : AdviceT,
		thePointcut : Pointcut[F],
		ev : AdviceT <:< Advice[F, ResultT]
		)
		: StaticAspect.Aux[F, AdviceT, ResultT] =
		new StaticAspect[F, AdviceT] {
			override type ResultType = ResultT

			override implicit val advice : Advice[F, ResultType] = theAdvice
			override implicit val pointcut : Pointcut[F] = thePointcut
			}
}
