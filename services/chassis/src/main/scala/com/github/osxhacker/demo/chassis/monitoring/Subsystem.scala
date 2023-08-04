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


object Subsystem
{
	/// Implicit Conversions
	implicit val subsystemEq : Eq[Subsystem] = Eq.fromUniversalEquals
	implicit val subsystemShow : Show[Subsystem] = Show.show (_.name)
}

