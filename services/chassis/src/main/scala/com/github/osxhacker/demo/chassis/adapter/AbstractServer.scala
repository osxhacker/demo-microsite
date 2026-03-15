package com.github.osxhacker.demo.chassis.adapter

import scala.language.postfixOps
import scala.util.Try

import cats.Now
import cats.effect.{
	ExitCode,
	IO
	}

import cats.data.Kleisli
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import org.typelevel.log4cats.LoggerFactory

import com.github.osxhacker.demo.chassis.effect.{
	Advice,
	Aspect,
	DefaultAdvice
	}


/**
 * The '''AbstractServer''' type defines the common ancestor for __all__
 * [[com.monovore.decline.effect.CommandIOApp]]-based microservice
 * implementations.  Here, concerns such as component lifecycle management,
 * command-line parsing of what
 * [[com.github.osxhacker.demo.chassis.adapter.ProgramArguments.OperatingMode]]
 * to employ, and providing an appropriately configured
 * [[org.typelevel.log4cats.LoggerFactory]] are addressed.
 */
abstract class AbstractServer (
	protected val name : String,
	protected val description : String
	)
	extends CommandIOApp (
		name = name,
		header = description,
		helpFlag = true
		)
{
	/// Self Type Constraints
	this : ProgramArguments =>


	/// Class Imports
	import AbstractServer.ServerDefinition
	import ProgramArguments.OperatingMode
	import cats.syntax.applicative._


	/// Instance Properties
	implicit protected def loggerFactory : LoggerFactory[IO]

	private lazy val implementation = Kleisli (main).pure[Try]
	private lazy val lifecycle = Aspect[Try, ServerDefinition].static ()
	private lazy val managed : OperatingMode => IO[ExitCode] = mode =>
		lifecycle (Now (implementation)).map (_ map (_.run (mode)))
			.value
			.fold (IO raiseError, identity)


	/**
	 * This version of the main method is provided by the concrete microservice
	 * implementation and is guaranteed to have lifecycle management applied
	 * automatically.
	 */
	protected def main (mode : OperatingMode) : IO[ExitCode]


	/**
	 * @see [[com.monovore.decline.effect.CommandIOApp]]
	 */
	final override def main : Opts[IO[ExitCode]] = arguments () map managed
}


object AbstractServer
{
	/// Class Imports
	import ProgramArguments.OperatingMode


	/// Class Types
	/**
	 * The '''AdviceKleisliType''' `type` reifies the result type advised by
	 * the
	 * [[com.github.osxhacker.demo.chassis.adapter.AbstractServer.ServerDefinition]]
	 * [[com.github.osxhacker.demo.chassis.adapter.AbstractServer.LifecycleAdvice]].
	 */
	type AdviceKleisliType = Kleisli[IO, OperatingMode, ExitCode]


	/**
	 * The '''LifecycleAdvice''' type defines the contract for __all__
	 * [[com.github.osxhacker.demo.chassis.effect.Advice]] available to and/or
	 * used by an [[com.github.osxhacker.demo.chassis.adapter.AbstractServer]]
	 * implementation.
	 */
	trait LifecycleAdvice
		extends Advice[Try, AdviceKleisliType]
	{
		final protected type KleisliType = AdviceKleisliType


		/// Instance Properties
		implicit protected def logFactory : LoggerFactory[IO]


		/**
		 * The createKleisli method is a model of the FACTORY pattern and serves
		 * to encapsulate the details of [[cats.data.Kleisli]]s participating in
		 * defining
		 * [[com.github.osxhacker.demo.chassis.adapter.AbstractServer.LifecycleAdvice.KleisliType]]
		 * instances.
		 */
		@inline
		final protected def createKleisli[A] (f : OperatingMode => IO[A])
			: Kleisli[IO, OperatingMode, A] =
			Kleisli (f)
	}


	/**
	 * The '''ServerDefinition''' type defines the universal
	 * [[com.github.osxhacker.demo.chassis.effect.Advice]] applied to __every__
	 * concrete [[com.github.osxhacker.demo.chassis.adapter.AbstractServer]]
	 * implementation.
	 */
	final class ServerDefinition ()
		(
			implicit

			/// Needed for ''LifecycleAdvice'''
			override protected val logFactory : LoggerFactory[IO]
		)
		extends DefaultAdvice[Try, AdviceKleisliType] ()
			with LifecycleAdvice
			with KamonLifecycle
			with ConfigureServiceLogging


	object ServerDefinition
	{
		/// Implicit Conversions
		implicit def summon (implicit logFactory : LoggerFactory[IO])
			: ServerDefinition =
			new ServerDefinition ()
	}
}
