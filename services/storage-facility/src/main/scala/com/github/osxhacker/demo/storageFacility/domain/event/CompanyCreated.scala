package com.github.osxhacker.demo.storageFacility.domain.event

import io.scalaland.chimney
import monocle.macros.Lenses

import com.github.osxhacker.demo.chassis.domain.Slug
import com.github.osxhacker.demo.chassis.domain.entity._
import com.github.osxhacker.demo.chassis.domain.event.{
	Region,
	ServiceFingerprint
	}

import com.github.osxhacker.demo.chassis.monitoring.CorrelationId
import com.github.osxhacker.demo.storageFacility.domain._


/**
 * The '''CompanyCreated''' type is the Domain Object Model representation of an
 * event which is emitted when a
 * [[com.github.osxhacker.demo.storageFacility.domain.Company]] has been
 * successfully created.  Note that '''CompanyCreated''' is the __only__
 * [[com.github.osxhacker.demo.storageFacility.domain.Company]] event which will
 * have all properties of a
 * [[com.github.osxhacker.demo.storageFacility.domain.Company]].
 */
@Lenses ()
final case class CompanyCreated (
	override val region : Region,
	override val fingerprint : Option[ServiceFingerprint],
	override val correlationId : CorrelationId,
	override val id : Identifier[Company],
	val slug : Slug,
	val name : Company.Name,
	val status : CompanyStatus,
	val timestamps : ModificationTimes
	)
	extends CompanyEvent
{
	/// Class Imports
	import chimney.dsl._


	/**
	 * The toCompany method creates a
	 * [[com.github.osxhacker.demo.storageFacility.domain.Company]] instance
	 * from '''this''' event.
	 */
	def toCompany () : Company =
		this.into[Company]
			.transform
}

