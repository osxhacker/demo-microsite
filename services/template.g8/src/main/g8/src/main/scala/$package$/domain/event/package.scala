package $package$.domain.event

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
}

