package com.github.osxhacker.demo.chassis.effect

import java.util.concurrent.atomic.AtomicInteger

import cats.{
	Eval,
	Later
	}

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import kamon.Kamon
import kamon.testkit.InitAndStopKamonAfterAll
import kamon.trace.Span
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AsyncWordSpec

import com.github.osxhacker.demo.chassis.monitoring.metrics.{
	MetricsAdvice,
	ScopeInternalOperations,
	StartWorkflow
	}


/**
 * The '''AdviceSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.effect.Advice]] for fitness of purpose
 * and serves as an exemplar of its use.
 *
 * @see [[com.github.osxhacker.demo.chassis.effect.Pointcut]]
 */
final class AdviceSpec ()
	extends AsyncWordSpec
		with AsyncIOSpec
		with Diagrams
		with InitAndStopKamonAfterAll
{
	/// Class Imports
	import cats.syntax.applicative._


	/// Class Types
	sealed trait Countable
	{
		/// Instance Properties
		def counter : AtomicInteger


		protected def increment () : Int = counter.incrementAndGet ()
	}


	sealed trait IncrementAfter[F[_], A]
		extends Advice[F, A]
	{
		/// Self Type Constraints
		this : Countable =>


		abstract override def apply (fa : Eval[F[A]])
			(implicit pointcut : Pointcut[F])
			: Eval[F[A]] =
			pointcut.after (super.apply (fa)) {
				_ => increment ()
				}
	}


	sealed trait IncrementBefore[F[_], A]
		extends Advice[F, A]
	{
		/// Self Type Constraints
		this : Countable =>


		abstract override def apply (fa : Eval[F[A]])
			(implicit pointcut : Pointcut[F])
			: Eval[F[A]] =
			pointcut.before (super.apply (fa)) {
				() => increment ()
				}
	}


	sealed trait VerifyAdviceEnvironment[F[_], A]
		extends Advice[F, A]
	{
		abstract override def apply (fa : Eval[F[A]])
			(implicit pointcut : Pointcut[F])
			: Eval[F[A]] =
			pointcut.before (super.apply (fa)) (
				() => {
					assert (Kamon.currentContext ().nonEmpty ())
					assert (!Kamon.currentSpan ().isRemote)
					assert (!Kamon.currentSpan ().isEmpty)
					assert (
						Kamon.currentContext ().get (Span.Key).id === Kamon.currentSpan ().id
						)
					}
				)
	}


	final case class MultipleAdvice[F[_], A] (
		override val counter : AtomicInteger
		)
		extends DefaultAdvice[F, A]
			with Countable
			with MetricsAdvice[F, A]
			with IncrementAfter[F, A]
			with IncrementBefore[F, A]
			with VerifyAdviceEnvironment[F, A]
			with ScopeInternalOperations[F, A]
			with StartWorkflow[F, A]
	{
		override val component : String = "multiple-advice"
		override val operation : String = "advice-unit-test"
	}


	final case class SingleAdvice[F[_], A] (
		override val counter : AtomicInteger
		)
		extends DefaultAdvice[F, A]
			with IncrementBefore[F, A]
			with Countable



	"The Advice concept" must {
		"support application of a single cross-cutting concern" in {
			val text = "representative value in a container"
			val counter = new AtomicInteger ()
			val advice = SingleAdvice[IO, String] (counter)
			val result = advice (Later (text.pure[IO]))

			result.value map {
				value =>
					assert (value === text)
					assert (counter.get () === 1)
				}
			}

		"support application of multiple cross-cutting concerns" in {
			val text = "representative value in a container"
			val counter = new AtomicInteger ()
			val advice = MultipleAdvice[IO, String] (counter)
			val result = advice (Later (text.pure[IO]))

			result.value map {
				value =>
					assert (value === text)
					assert (counter.get () === 2)
				}
			}
		}
}

