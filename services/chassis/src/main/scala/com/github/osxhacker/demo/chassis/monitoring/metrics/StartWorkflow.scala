package com.github.osxhacker.demo.chassis.monitoring.metrics

import scala.language.postfixOps

import cats.Eval
import kamon.Kamon
import kamon.context.Context
import kamon.trace.{
	Span,
	SpanBuilder
	}

import com.github.osxhacker.demo.chassis.effect.{
	Advice,
	Pointcut
	}


/**
 * The '''StartWorkflow''' type defines
 * [[com.github.osxhacker.demo.chassis.effect.Advice]] which establishes a new
 * [[kamon.context.Context]] on entry to `apply` and `close`s it after each
 * ''F[A]'' is produced.  As such, '''StartWorkflow''' typically should
 * be the __last__ [[com.github.osxhacker.demo.chassis.effect.Advice]]
 * introduced so that the [[kamon.context.Context]] is stored __before__ other
 * advice decorates each ''F[A]''.
 */
trait StartWorkflow[F[_], ResultT]
	extends Advice[F, ResultT]
{
	/// Self Type Constraints
	this : MetricsAdvice[F, ResultT] =>


	/// Class Imports
	import MetricsAdvice.ManagedSpan


	/// Instance Properties
	def component : String
	def initialContext : Context = Context.Empty
	def operation : String


	abstract override def apply (fa : Eval[F[ResultT]])
		(implicit pointcut : Pointcut[F])
		: Eval[F[ResultT]] =
		pointcut.bracket (super.apply (fa)) (
			() => {
				val span = spanBuilder ().context (initialContext)
					.tag (tags)
					.start ()

				val scope = Kamon.storeContext (
					initialContext.withEntry (Span.Key, span)
					)

				ManagedSpan (span, scope)
				}
			) (span => _.fold (span.failWith, _ => span.cleanup ()))


	private def spanBuilder () : SpanBuilder =
		Kamon.clientSpanBuilder (operation, component)
}

