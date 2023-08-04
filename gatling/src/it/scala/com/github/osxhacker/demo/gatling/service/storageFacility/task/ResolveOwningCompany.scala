package com.github.osxhacker.demo.gatling.service.storageFacility.task

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.structure.ChainBuilder

import com.github.osxhacker.demo.api
import com.github.osxhacker.demo.gatling.ServiceEndpoint
import com.github.osxhacker.demo.gatling.service.{
	Endpoints,
	company
	}

import com.github.osxhacker.demo.gatling.service.company.CompanySessionKeys
import com.github.osxhacker.demo.gatling.service.storageFacility.StorageFacilitySessionKeys


/**
 * The '''ResolveOwningCompany''' type defines the
 * [[com.github.osxhacker.demo.gatling.service.storageFacility.task.StorageFacilityTask]]
 * responsible for resolving the
 * [[com.github.osxhacker.demo.gatling.service.storageFacility.StorageFacilitySessionKeys.OwningCompanyEntry]]
 * required for subsequent
 * [[com.github.osxhacker.demo.gatling.service.storageFacility.task.StorageFacilityTask]]s.
 */
final case class ResolveOwningCompany (
	private val endpoint : ServiceEndpoint
	)
	(implicit override val configuration: GatlingConfiguration)
	extends StorageFacilityTask ()
{
	/// Class Imports
	import io.gatling.commons.validation._


	/// Instance Properties
	private val findAllCompanies = company.task.FindAllCompanies (
		endpoint = Endpoints.company,
		companiesKey = CompanySessionKeys.CompaniesEntry
		)


	def apply (slug : String) : ChainBuilder = exec (findAllCompanies ())
		.exec {
			session =>
				CompanySessionKeys.CompaniesEntry
					.session
					.andThen (company.ResourceOptics.companies.companies)
					.andThen (company.ResourceOptics.companies.findBySlug (slug))
					.some
					.headOption (session)
					.toValidation (s"unable to find company by '$slug''")
					.map {
						StorageFacilitySessionKeys.OwningCompanyEntry
							.add (_) (session)
						}
			}
		.exec (CompanySessionKeys.CompaniesEntry.remove ())
		.exitHereIfFailed
}

