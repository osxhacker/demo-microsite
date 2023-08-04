package com.github.osxhacker.demo.storageFacility.domain.event

import io.scalaland.chimney
import monocle.macros.Lenses

import com.github.osxhacker.demo.storageFacility.domain._
import com.github.osxhacker.demo.chassis.domain.entity.Identifier
import com.github.osxhacker.demo.chassis.domain.event.{
	Region,
	ServiceFingerprint
	}

import com.github.osxhacker.demo.chassis.monitoring.CorrelationId


/**
 * The '''CompanyDeleted''' type is the Domain Object Model representation of an
 * event which is emitted when a
 * [[com.github.osxhacker.demo.storageFacility.domain.Company]] has been
 * successfully removed from the persistent store.
 */
@Lenses ()
final case class CompanyDeleted (
	override val region : Region,
	override val fingerprint : Option[ServiceFingerprint],
	override val correlationId : CorrelationId,
	override val id : Identifier[Company]
	)
	extends CompanyEvent

