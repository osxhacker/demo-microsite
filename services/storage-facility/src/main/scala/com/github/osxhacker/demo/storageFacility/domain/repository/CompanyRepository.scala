package com.github.osxhacker.demo.storageFacility.domain.repository

import com.github.osxhacker.demo.chassis.domain.repository.Repository
import com.github.osxhacker.demo.storageFacility.domain.{
	Company,
	CompanyReference
	}


/**
 * The '''CompanyRepository''' type defines the
 * [[com.github.osxhacker.demo.chassis.domain.repository.Repository]]
 * contract for managing the persistent store representation of
 * [[com.github.osxhacker.demo.storageFacility.domain.Company]] instances.
 */
trait CompanyRepository[F[_]]
	extends Repository[F, Company]
{
	/**
	 * The createSchema method will create each
	 * [[com.github.osxhacker.demo.storageFacility.domain.Company]]
	 * related table if it does not already exist.  The returned ''Int'' is a
	 * count of how many statements were ran.
	 */
	def createSchema () : F[Int]


	/**
	 * The find method attempts to retrieve a
	 * [[com.github.osxhacker.demo.storageFacility.domain.Company]] by a unique
	 * [[com.github.osxhacker.demo.storageFacility.domain.CompanyReference]],
	 * raising an error in ''F'' if it fails.
	 */
	def find (reference : CompanyReference) : F[Company]
}

