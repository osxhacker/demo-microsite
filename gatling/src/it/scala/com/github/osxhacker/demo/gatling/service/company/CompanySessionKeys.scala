package com.github.osxhacker.demo.gatling.service.company

import scala.language.{
	implicitConversions,
	postfixOps
	}

import enumeratum._

import com.github.osxhacker.demo.api
import com.github.osxhacker.demo.gatling.SessionKey


/**
 * The '''CompanySessionKeys''' type defines __all__ known
 * [[io.gatling.core.session.Session]] keys available when interacting with
 * the Company service.
 */
sealed trait CompanySessionKeys
	extends SessionKey


object CompanySessionKeys
	extends Enum[CompanySessionKeys]
{
	/// Class Types
	case object AuthTokenEntry
		extends SessionKey.Definition[CompanySessionKeys, String]
			with CompanySessionKeys


	case object CompaniesEntry
		extends SessionKey.Definition[CompanySessionKeys, api.company.Companies]
			with CompanySessionKeys


	case object GeneratedSlugEntry
		extends SessionKey.Definition[CompanySessionKeys, String]
			with CompanySessionKeys


	case object TargetCompanyEntry
		extends SessionKey.Definition[CompanySessionKeys, api.company.Company]
			with CompanySessionKeys


	/// Instance Properties
	override val values : IndexedSeq[CompanySessionKeys] = findValues


	/// Implicit Conversions
	implicit def companySessionKeysToString[A <: CompanySessionKeys] (
		instance : A
		)
		: String =
		instance.entryName
}

