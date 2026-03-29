package com.github.osxhacker.demo.chassis.monitoring.logging


/**
 * The '''Loggable''' type defines the `logging` subsystem concept of a type
 * which can participate in the production of system activity recorded in a
 * persistent store.  This contract defines the __minimal__ collaborators
 * required to participate.
 */
trait Loggable
{
	/// Instance Properties
	/**
	 * The cause property represents an [[scala.Option]]al error encountered as
	 * represented by a [[java.lang.Throwable]] instance.
	 */
	def cause : Option[Throwable] = None

	/**
	 * The context property provides additional information related to associate
	 * with '''this''' instance.
	 */
	def context : Map[String, String] = Map.empty

	/**
	 * The message property is required to contain a succinct description of the
	 * information to be logged.  It can be multi-line and should have
	 * sufficient information within it such that it assists in reasoning about
	 * system behavior.
	 */
	def message : String
}

