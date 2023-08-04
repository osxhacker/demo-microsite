package com.github.osxhacker.demo.chassis.domain.event

import enumeratum.EnumEntry


/**
 * The '''Channel''' type defines the contract for identifying discrete event
 * distribution mechanisms, such as Kafka topics and JMS queues/topics.  With
 * it, [[com.github.osxhacker.demo.chassis.domain.event.EventConsumer]]s and
 * [[com.github.osxhacker.demo.chassis.domain.event.EventProducer]]s can
 * determine technology-specific settings and where to route events.
 *
 * Each '''Channel''' `entryName` is converted from the [[enumeratum.EnumEntry]]
 * default into a lower-case hyphenated value.
 */
trait Channel
	extends EnumEntry
		with EnumEntry.Hyphencase
