package $package$.domain

import com.softwaremill.diffx.Diff
import enumeratum._


/**
 * The '''CompanyStatus''' type defines the Domain Object Model
 * representation of __all__ discrete
 * [[$package$.domain.Company]] status
 * indicators.
 */
sealed trait CompanyStatus
	extends EnumEntry
{
}


object CompanyStatus
	extends Enum[CompanyStatus]
		with CatsEnum[CompanyStatus]
{
	/// Class Types
	case object Active
		extends CompanyStatus


	case object Inactive
		extends CompanyStatus


	case object Suspended
		extends CompanyStatus


	/// Instance Properties
	val values : IndexedSeq[CompanyStatus] = findValues


	/// Implicit Conversions
	implicit val companyStatusDiff : Diff[CompanyStatus] =
		Diff.derived[CompanyStatus]
}

