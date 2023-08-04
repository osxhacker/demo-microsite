package com.github.osxhacker.demo.storageFacility.domain.event

import monocle.macros.Lenses

import com.github.osxhacker.demo.storageFacility.domain._
import com.github.osxhacker.demo.chassis.domain.entity.Identifier
import com.github.osxhacker.demo.chassis.domain.event.{
	Region,
	ServiceFingerprint
	}

import com.github.osxhacker.demo.chassis.monitoring.CorrelationId


/**
 * The '''CompanyStatusChanged''' type is the Domain Object Model representation
 * of an event which is emitted when a
 * [[com.github.osxhacker.demo.storageFacility.domain.Company]] has its
 * [[com.github.osxhacker.demo.storageFacility.domain.CompanyStatus]] altered.
 */
@Lenses ()
final case class CompanyStatusChanged (
	override val region : Region,
	override val fingerprint : Option[ServiceFingerprint],
	override val correlationId : CorrelationId,
	override val id : Identifier[Company],
	val status : CompanyStatus
	)
	extends CompanyEvent

