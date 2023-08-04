package com.github.osxhacker.demo.storageFacility.domain

import cats.Eq
import com.softwaremill.diffx
import com.softwaremill.diffx.Diff
import eu.timepit.refined
import monocle.macros.Lenses

import com.github.osxhacker.demo.chassis.domain.Slug
import com.github.osxhacker.demo.chassis.domain.entity.{
	Identifier,
	ModificationTimes
	}


/**
 * The '''Company''' type is the Domain Object Model representation of an
 * organization known to the system.  For the purposes of this microservice, it
 * is a member of the "reference model" and only supports mutation by way of
 * company related [[com.github.osxhacker.demo.storageFacility.domain.event]]s.
 *
 * Only properties relevant to this bounded context are retained.
 */
@Lenses ()
final case class Company (
	val id : Identifier[Company],
	val slug : Slug,
	val name : Company.Name,
	val status : CompanyStatus,
	val timestamps : ModificationTimes
	)
{
	/**
	 * The toRef method creates a
	 * [[com.github.osxhacker.demo.storageFacility.domain.CompanyReference]]
	 * for '''this''' instance.
	 */
	def toRef () : CompanyReference = CompanyReference (slug, id)
}


object Company
{
	/// Class Imports
	import diffx.refined._
	import refined.auto._
	import refined.api.Refined
	import refined.string.Trimmed


	/// Class Types
	type Name = Refined[
		String,
		Trimmed
		]

	/// Implicit Conversions
	implicit val companyDiff : Diff[Company] = Diff.derived[Company]
	implicit val companyEq : Eq[Company] = Eq.fromUniversalEquals
	implicit val companyNamespace : Identifier.EntityNamespace[Company] =
		Identifier.namespaceFor[Company] ("company")
}

