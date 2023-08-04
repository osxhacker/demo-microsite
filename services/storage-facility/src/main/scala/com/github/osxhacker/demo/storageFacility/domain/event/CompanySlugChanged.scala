package com.github.osxhacker.demo.storageFacility.domain.event

import io.scalaland.chimney
import monocle.macros.Lenses

import com.github.osxhacker.demo.chassis.domain.Slug
import com.github.osxhacker.demo.chassis.domain.entity.Identifier
import com.github.osxhacker.demo.chassis.domain.event.{
	Region,
	ServiceFingerprint
	}

import com.github.osxhacker.demo.chassis.monitoring.CorrelationId
import com.github.osxhacker.demo.storageFacility.domain._


/**
 * The '''CompanySlugChanged''' type is the Domain Object Model representation
 * of an event which is emitted when a
 * [[com.github.osxhacker.demo.storageFacility.domain.Company]] has its
 * [[com.github.osxhacker.demo.chassis.domain.Slug]] altered.
 */
@Lenses ()
final case class CompanySlugChanged (
	override val region : Region,
	override val fingerprint : Option[ServiceFingerprint],
	override val correlationId : CorrelationId,
	override val id : Identifier[Company],
	val from : Slug,
	val to : Slug
	)
	extends CompanyEvent

