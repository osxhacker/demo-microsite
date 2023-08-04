package com.github.osxhacker.demo.company.domain

import shapeless.{
	syntax => _,
	_
	}


/**
 * The '''event''' `package` defines Domain Object Model types responsible for
 * being the source of model notifications.
 */
package object event
{
	/// Class Types
	/**
	 * The '''AllCompanyEvents''' type defines __all possible events__ the
	 * company domain can produce.
	 */
	type AllCompanyEvents =
		CompanyCreated :+:
		CompanyDeleted :+:
		CompanyChangeEvents


	/**
	 * The '''CompanyChangeEvents''' type defines __all__ existing
	 * [[com.github.osxhacker.demo.company.domain.Company]] modification events
	 * the company domain can produce.
	 */
	type CompanyChangeEvents =
		CompanyProfileChanged :+:
		CompanySlugChanged :+:
		CompanyStatusChanged :+:
		CNil
}

