package com.github.osxhacker.demo.chassis.effect

import scala.concurrent.{
	ExecutionContext,
	Future
	}

import scala.util.{
	Failure,
	Success,
	Try
	}

import cats.Later
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AsyncWordSpec


/**
 * The '''PointcutSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.effect.Pointcut]] for fitness of purpose
 * and serves as an exemplar of its use.
 *
 * @see [[com.github.osxhacker.demo.chassis.effect.IOPointcutSpec]]
 */
final class PointcutSpec ()
	extends AsyncWordSpec
		with Diagrams
{
	/// Class Imports
	import ExecutionContext.global
	import cats.syntax.applicative._
	import cats.syntax.either._


	"The Pointcut type class" must {
		"support synchronous applicative types" in {
			assertCompiles (
				"""
				val errorOrPointcut : Pointcut[Try] = implicitly[Pointcut[Try]]
				"""
				)
			}

		"support asynchronous applicative types" in {
			assertCompiles (
				"""
				val errorOrPointcut : Pointcut[Future] = implicitly[Pointcut[Future]]
				"""
				)
			}

		"be able to invoke logic 'before'" in {
			var boolean = false
			val result = Pointcut[Try] ().before (Later (1.pure[Try])) (
				() => boolean = true
				)
				.value

			assert (result.isSuccess)
			assert (boolean)
			}

		"be able to invoke logic 'after'" in {
			var int = 0
			val result = Pointcut[Try] ().after (Later (1.pure[Try])) (
				int = _
				)
				.value

			assert (result.isSuccess)
			assert (int === 1)
			}

		"be able to invoke logic 'around' (Try)" in {
			val sequence = new StringBuilder ()
			val result = Pointcut[Try] ().around (Later (1.pure[Try])) (
				entering = () => sequence.append ("entering, "),
				leaving = sequence.append ("leaving: ").append (_)
				)
				.value

			assert (result.isSuccess)
			assert (sequence.toString () === "entering, leaving: 1")
			}

		"be able to invoke logic 'around' (Future)" in {
			var before = false
			var after = 0
			val result = Pointcut[Future] ().around (Later (1.pure[Future])) (
				entering = () => before = true,
				leaving = a => after = a
				)
				.value

			result map {
				value =>
					assert (before)
					assert (after === value)
				}
			}

		"be able to detect errors (Try)" in {
			var after = false
			var hadError = false
			val result = Pointcut[Try] ().always (
				Later (Failure (new Exception ("boom!")))
				) (_ => after = true, _ => hadError = true)
				.value

			assert (result.isFailure)
			assert (after === false)
			assert (hadError === true)
			}

		"be able to detect errors (Future)" in {
			var after = false
			var hadError = false
			val result = Pointcut[Future] ().always (
				Later (
					Future.failed[Int] (new Exception ("boom!"))
					)
				) (_ => after = true, _ => hadError = true)
				.value

			result.recover[Int] (_ => 0).map {
				_ =>
					assert (after === false)
					assert (hadError === true)
				}
			}
		}
}

