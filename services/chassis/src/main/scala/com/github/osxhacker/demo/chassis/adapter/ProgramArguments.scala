package com.github.osxhacker.demo.chassis.adapter

import com.monovore.decline._
import eu.timepit.refined


/**
 * The '''ProgramArguments''' type defines the minimal set of program
 * command-line invocation arguments supported by __all__ microservices.
 */
trait ProgramArguments
{
	/// Class Imports
	import ProgramArguments._
	import cats.implicits._
	import refined.auto._


	/// Instance Properties
	private val docker = Opts.flag (
		long = "docker",
		help = "Enable docker execution configuration"
		)
		.map (_ => "docker.conf" : ConfigurationName)

	private val json = Opts.flag (
		long = "json",
		help = "Enable JSON logging format (default)"
		)
		.map (_ => "json")
		.withDefault ("json")

	private val native = Opts.flag (
		long = "native",
		help = "Enable native execution configuration (default)"
		)
		.map (_ => "application.conf" : ConfigurationName)
		.withDefault ("application.conf" : ConfigurationName)

	private val plain = Opts.flag (
		long = "plain",
		help = "Enable plain-text logging format"
		)
		.map (_ => "plain")

	private val verbose = Opts.flag (
		long = "verbose",
		short = "v",
		help = "Enable verbose (debug) logging"
		)
		.orFalse

	private val dockerMode = (docker, plain orElse json, verbose).mapN (
		DockerInvocation
		)

	private val nativeMode = (native, plain orElse json, verbose).mapN (
		NativeInvocation
		)


	/**
	 * The arguments method determines what operating mode has been requested of
	 * the service based on the command line options given.
	 */
	def arguments () : Opts[Product] = dockerMode orElse nativeMode
}


object ProgramArguments
{
	/// Class Imports
	import refined.api.Refined
	import refined.string.MatchesRegex


	/// Class Types
	type ConfigurationName = Refined[
		String,
		MatchesRegex["""^[A-Za-z0-9][A-Za-z0-9._-]*[A-Za-z0-9._-]?\.conf$"""]
		]


	sealed trait OperatingMode
	{
		/// Instance Properties
		def settings : ConfigurationName
		def loggingLayout : String
		def verbose : Boolean
	}


	final case class DockerInvocation (
		override val settings : ConfigurationName,
		override val loggingLayout : String,
		override val verbose : Boolean
		)
		extends OperatingMode


	final case class NativeInvocation (
		override val settings : ConfigurationName,
		override val loggingLayout : String,
		override val verbose : Boolean
		)
		extends OperatingMode
}
