package com.github.osxhacker.demo.gatling.service.storageFacility.task

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
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
 * The '''InitializeSystem''' type defines the
 * [[com.github.osxhacker.demo.gatling.service.storageFacility.task.StorageFacilityTask]]
 * responsible for ensuring the system is in a known initial state.  It can be
 * used before all and/or each of a simulation's
 * [[https://gatling.io/docs/gatling/reference/current/core/scenario/ scenarios]].
 */
final case class InitializeSystem (
	private val endpoint : ServiceEndpoint,
	private val owningSlug : String
	)
	(implicit override val configuration : GatlingConfiguration)
	extends StorageFacilityTask ()
{
	/// Class Imports
	import io.gatling.commons.validation._


	/// Class Types
	private object companyTasks
	{
		/// Class Imports
		import company.CompanySessionKeys


		/// Instance Properties
		lazy val create = company.task.CreateCompany (Endpoints.company)
		lazy val delete = company.task.DeleteCompany (Endpoints.company)
		lazy val findAll = company.task.FindAllCompanies (
			companiesKey = CompanySessionKeys.CompaniesEntry,
			endpoint = Endpoints.company
			)
	}


	/// Instance Properties
	private val allGatlingCompanies : Session => List[api.company.Company] =
		CompanySessionKeys.CompaniesEntry
			.session
			.getAll (_)
			.flatMap (_.companies)
			.filter (_.slug.startsWith ("gatling"))


	def apply () : ChainBuilder = exec (logSession ("Initializing:"))
		.exec (companyTasks.findAll ())
		.foreach (allGatlingCompanies, CompanySessionKeys.TargetCompanyEntry) {
			companyTasks.delete (CompanySessionKeys.TargetCompanyEntry)
			}
		.exec (CompanySessionKeys.CompaniesEntry.remove ())
		.exec (
			companyTasks.create (owningSlug, api.company.CompanyStatus.Active)
			)
		.exec {
			session =>
				CompanySessionKeys.TargetCompanyEntry
					.session
					.headOption (session)
					.toValidation ("unable to resolve newly created company")
					.map {
						StorageFacilitySessionKeys.OwningCompanyEntry
							.add (_) (session)
						}
			}
		.exec (CompanySessionKeys.TargetCompanyEntry.remove ())
		.exec (logSession ("Finished initialization:"))
}

