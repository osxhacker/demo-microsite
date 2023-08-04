package com.github.osxhacker.demo.storageFacility.domain

import shapeless.{
	syntax => _,
	_
	}


/**
 * The '''event''' `package` defines Domain Object Model types responsible for
 * being the source of model notifications.  This includes both emission and
 * consumption from other microservices.
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
		CompanyProfileChanged :+:
		CompanySlugChanged :+:
		CompanyStatusChanged :+:
		CNil


	/**
	 * The '''AllStorageFacilityEvents''' type defines __all possible events__
	 * the storage facility domain can produce.
	 */
	type AllStorageFacilityEvents =
		StorageFacilityCreated :+:
		StorageFacilityDeleted :+:
		StorageFacilityChangeEvents


	/**
	 * The '''CompanyChangeEvents''' type defines __all__ existing
	 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]]
	 * modification events the company domain can produce.
	 */
	type StorageFacilityChangeEvents =
		StorageFacilityProfileChanged :+:
		StorageFacilityStatusChanged :+:
		CNil
}

