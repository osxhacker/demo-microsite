package com.github.osxhacker.demo.gatling.service.company

import java.util.UUID.randomUUID

import com.github.osxhacker.demo.api
import com.github.osxhacker.demo.gatling.FeatureSimulation


/**
 * The '''CompanyFeatures''' type is a minimal
 * [[com.github.osxhacker.demo.gatling.FeatureSimulation]] intended to both
 * verify minimal functionality of the Company service as well as serve as a
 * "proving grounds" for additional
 * [[https://gatling.io/docs/gatling/tutorials/quickstart/ Gatling]] support.
 */
final class CompanyFeatures ()
	extends FeatureSimulation ("http://localhost:6891")
		with ResourceOptics
{
	/// Class Imports
	import io.gatling.commons.validation._


	/// Class Types
	private object tasks
	{
		/// Instance Properties
		val create = task.CreateCompany (serviceEndpoint)
		val delete = task.DeleteCompany (endpoint = serviceEndpoint)
		val findAll = task.FindAllCompanies (
			endpoint = serviceEndpoint,
			companiesKey = CompanySessionKeys.CompaniesEntry
			)
	}


	/// Instance Properties
	lazy val addCompany = scenario ("Create Company").exec (
		tasks.create (generatedSlug, api.company.CompanyStatus.Inactive)
		)

	lazy val allCompanies = scenario ("Retrieve All Companies").exec (
		tasks.findAll ()
		)

	lazy val delete = scenario ("Delete Company")
		.exec (tasks.findAll (minimumExisting = 1))
		.exitHereIfFailed
		.exec {
			session =>
				val company = CompanySessionKeys.CompaniesEntry
					.session
					.andThen (companies.companies)
					.andThen (companies.findBySlug (generatedSlug))
					.some
					.headOption (session)
					.toValidation ("unable to find company by generated slug")

				company.flatMap {
					CompanySessionKeys.TargetCompanyEntry
						.add (_) (session)
						.success
					}
			}
		.exec (tasks.delete (CompanySessionKeys.TargetCompanyEntry))

	private val generatedSlug = s"gatling-${randomUUID ()}"


	/// Constructor Body
	evaluate (protocols.json) {
		addCompany ::
		allCompanies ::
		delete ::
		Nil
		}
}

