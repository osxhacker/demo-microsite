package com.github.osxhacker.demo.company.domain.event

import enumeratum._

import com.github.osxhacker.demo.chassis.domain.event.Channel


/**
 * The '''EventChannel''' type defines what
 * [[com.github.osxhacker.demo.chassis.domain.event.Channel]]s are known and
 * supported by the company microservice.
 */
sealed trait EventChannel
	extends Channel


object EventChannel
	extends Enum[EventChannel]
		with CatsEnum[EventChannel]
{
	/// Class Types
	case object Company
		extends EventChannel


	case object UnitTest
		extends EventChannel


	/// Instance Properties
	override val values = findValues
}

