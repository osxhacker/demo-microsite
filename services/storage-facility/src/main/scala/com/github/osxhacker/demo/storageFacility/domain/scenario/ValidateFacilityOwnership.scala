package com.github.osxhacker.demo.storageFacility.domain.scenario

import cats.data.ValidatedNec

import com.github.osxhacker.demo.storageFacility.domain.{
	Company,
	CompanyReference,
	StorageFacility
	}


/**
 * The '''ValidateFacilityOwnership''' `object` defines the algorithm for
 * ensuring a
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]]
 * `belongsTo` a specific
 * [[com.github.osxhacker.demo.storageFacility.domain.Company]].  If it does
 * not, an error is raised in the context of ''F''.
 */
private[scenario] object ValidateFacilityOwnership
{
	/// Class Imports
	import mouse.boolean._


	/// Class Types
	type F[+A] = ValidatedNec[String, A]


	def apply (facility : StorageFacility, company : Company)
		: F[StorageFacility] =
		apply (facility, company.toRef ())


	def apply (facility : StorageFacility, company : CompanyReference)
		: F[StorageFacility] =
		facility.belongsTo (company)
			.validatedNec (explanation (facility), facility)


	private def explanation (facility : StorageFacility) : String =
		new StringBuilder ()
			.append ("facility is owned by different company: '")
			.append (StorageFacility.id.get (facility).toUrn ())
			.append ('\'')
			.toString ()
}

