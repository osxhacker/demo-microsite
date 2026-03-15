package com.github.osxhacker.demo.chassis.adapter

import scala.annotation.unused
import scala.language.postfixOps
import scala.util.Try

import cats.Eval
import cats.effect.IO

import com.github.osxhacker.demo.chassis.effect.Pointcut


/**
 * The '''ConfigureServiceLogging''' type defines the contract for ensuring
 * Logback environment properties are set as specified by the settings in
 * [[com.github.osxhacker.demo.chassis.adapter.ProgramArguments.OperatingMode]].
 */
trait ConfigureServiceLogging
	extends AbstractServer.LifecycleAdvice
{
	/// Class Imports
	import cats.syntax.flatMap._
	import mouse.boolean._


	/// Instance Properties
	private val configure = createKleisli {
		mode =>
			IO.delay (
				System.setProperty (
					"LOG_LAYOUT",
					mode.loggingLayout.toUpperCase
					)
				) >>
			IO.delay (
				System.setProperty (
					"LOG_LEVEL",
					mode.verbose.fold ("DEBUG", "INFO")
					)
				)
		}


	abstract override def apply (efa : Eval[Try[KleisliType]])
		(
			implicit

			@unused
			pointcut : Pointcut[Try]
		)
		: Eval[Try[KleisliType]] =
		super.apply (efa) map (_ map (configure >> _))
}

