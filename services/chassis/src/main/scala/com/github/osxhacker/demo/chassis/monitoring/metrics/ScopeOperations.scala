package com.github.osxhacker.demo.chassis.monitoring.metrics

import scala.language.postfixOps

import cats.Eval
import kamon.Kamon
import kamon.trace.{
	Span,
	SpanBuilder
	}

import com.github.osxhacker.demo.chassis.effect.{
	Advice,
	Pointcut
	}


/**
 * The '''ScopeOperations''' type defines the parent
 * [[com.github.osxhacker.demo.chassis.monitoring.metrics.MetricsAdvice]] which
 * establishes a monitoring scope such that metrics captured within it are
 * associated with a symbolic `operation` name.  Sub-types within this
 * translation unit establish the [[kamon.trace.Span.Kind]] of
 * [[kamon.trace.Span]] used.
 */
sealed trait ScopeOperations[F[_], ResultT]
	extends Advice[F, ResultT]
{
	/// Self Type Constraints
	this : MetricsAdvice[F, ResultT] =>


	/// Class Imports
	import MetricsAdvice.ManagedSpan


	/// Instance Properties
	def component : String
	def operation : String


	abstract override def apply (fa : Eval[F[ResultT]])
		(implicit pointcut : Pointcut[F])
		: Eval[F[ResultT]] =
		pointcut.bracket (super.apply (fa)) (
			() => {
				val context = Kamon.currentContext ()
				val parent = Kamon.currentSpan ()
				val span = spanBuilder ().context (context)
					.asChildOf (parent)
					.tag (tags)
					.start ()

				val scope = Kamon.storeContext (
					context.withEntry (Span.Key, span)
					)

				ManagedSpan (span, scope)
				}
			) (span => _.fold (span.failWith, _ => span.cleanup ()))


	protected def spanBuilder () : SpanBuilder
}


/**
 * The '''ScopeClientOperations''' type ensures that the
 * [[com.github.osxhacker.demo.chassis.monitoring.metrics.ScopeOperations]]
 * [[kamon.trace.Span]] is a [[kamon.trace.Span.Kind.Client]].
 */
trait ScopeClientOperations[F[_], ResultT]
	extends ScopeOperations[F, ResultT]
{
	/// Self Type Constraints
	this : MetricsAdvice[F, ResultT] =>


	final override protected def spanBuilder () : SpanBuilder =
		Kamon.clientSpanBuilder (operation, component)
			.tag (Span.TagKeys.SpanKind, Span.Kind.Client.toString ())
}


/**
 * The '''ScopeConsumerOperations''' type ensures that the
 * [[com.github.osxhacker.demo.chassis.monitoring.metrics.ScopeOperations]]
 * [[kamon.trace.Span]] is a [[kamon.trace.Span.Kind.Consumer]].
 */
trait ScopeConsumerOperations[F[_], ResultT]
	extends ScopeOperations[F, ResultT]
{
	/// Self Type Constraints
	this : MetricsAdvice[F, ResultT] =>


	final override protected def spanBuilder () : SpanBuilder =
		Kamon.consumerSpanBuilder (operation, component)
			.tag (Span.TagKeys.SpanKind, Span.Kind.Consumer.toString ())
}


/**
 * The '''ScopeInternalOperations''' type ensures that the
 * [[com.github.osxhacker.demo.chassis.monitoring.metrics.ScopeOperations]]
 * [[kamon.trace.Span]] is a [[kamon.trace.Span.Kind.Internal]].
 */
trait ScopeInternalOperations[F[_], ResultT]
	extends ScopeOperations[F, ResultT]
{
	/// Self Type Constraints
	this : MetricsAdvice[F, ResultT] =>


	final override protected def spanBuilder () : SpanBuilder =
		Kamon.internalSpanBuilder (operation, component)
			.tag (Span.TagKeys.SpanKind, Span.Kind.Internal.toString ())
}


/**
 * The '''ScopeProducerOperations''' type ensures that the
 * [[com.github.osxhacker.demo.chassis.monitoring.metrics.ScopeOperations]]
 * [[kamon.trace.Span]] is a [[kamon.trace.Span.Kind.Producer]].
 */
trait ScopeProducerOperations[F[_], ResultT]
	extends ScopeOperations[F, ResultT]
{
	/// Self Type Constraints
	this : MetricsAdvice[F, ResultT] =>


	final override protected def spanBuilder () : SpanBuilder =
		Kamon.producerSpanBuilder (operation, component)
			.tag (Span.TagKeys.SpanKind, Span.Kind.Producer.toString ())
}


/**
 * The '''ScopeServerOperations''' type ensures that the
 * [[com.github.osxhacker.demo.chassis.monitoring.metrics.ScopeOperations]]
 * [[kamon.trace.Span]] is a [[kamon.trace.Span.Kind.Server]].
 */
trait ScopeServerOperations[F[_], ResultT]
	extends ScopeOperations[F, ResultT]
{
	/// Self Type Constraints
	this : MetricsAdvice[F, ResultT] =>


	final override protected def spanBuilder () : SpanBuilder =
		Kamon.serverSpanBuilder (operation, component)
			.tag (Span.TagKeys.SpanKind, Span.Kind.Server.toString ())
}

