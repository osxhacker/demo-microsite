package com.github.osxhacker.demo.chassis.monitoring

import cats.{
	Eq,
	Show
	}


/**
 * The '''Subsystem''' type reifies the concept of a distinct self-contained
 * group of logic which interacts with the same technology.  Kafka, REST, and
 * databases for example.
 */
final case class Subsystem (val name : String)
{
	/**
	 * The addTo method incorporates the value of '''this''' '''Subsystem'''
	 * into the contents of the given ''Map'' '''instance'''.
	 */
	def addTo (instance : Map[String, String]) : Map[String, String] =
		instance.updated ("subsystem", name)
}


object Subsystem
{
	/// Implicit Conversions
	implicit val subsystemEq : Eq[Subsystem] = Eq.fromUniversalEquals
	implicit val subsystemShow : Show[Subsystem] = Show.show (_.name)
}

