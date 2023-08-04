package com.github.osxhacker.demo.company.adapter.kafka

import enumeratum._


/**
 * The '''IntegrationSettings''' type defines the location of the
 * [[com.github.osxhacker.demo.company.adapter.RuntimeSettings]]s corresponding
 * to each [[com.github.osxhacker.demo.company.adapter.kafka.IntegrationSpec]]
 * defined.
 */
sealed trait IntegrationSettings
	extends EnumEntry
		with EnumEntry.Hyphencase
{
	/// Instance Properties
	val configuration : String = entryName + ".conf"
}


object IntegrationSettings
	extends Enum[IntegrationSettings]
		with CatsEnum[IntegrationSettings]
{
	/// Class Types
	case object ConsumeCompanyEvents
		extends IntegrationSettings


	case object PublishCompanyEvents
		extends IntegrationSettings


	/// Instance Properties
	override val values = findValues
}

