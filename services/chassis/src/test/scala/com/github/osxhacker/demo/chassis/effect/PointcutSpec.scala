package com.github.osxhacker.demo.chassis.effect

import scala.concurrent.Future
import scala.util.Try

import org.scalatest.Assertion
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
		with PointcutBehaviours
{
	/// Instance Properties
	implicit private val tryToFuture : Try[Assertion] => Future[Assertion] =
		Future.fromTry


	"The Pointcut type class" when {
		"bound to Future" must {
			"support asynchronous applicative types" in {
				assertCompiles (
					"""
					val errorOrPointcut : Pointcut[Future] =
						implicitly[Pointcut[Future]]
					"""
					)
				}

			behave like functionalAOP[Future] ()
			}

		"bound to Try" must {
			"support synchronous applicative types" in {
				assertCompiles (
					"""
					val errorOrPointcut : Pointcut[Try] =
						implicitly[Pointcut[Try]]
					"""
					)
				}

			behave like functionalAOP[Try] ()
			}
	}
}

