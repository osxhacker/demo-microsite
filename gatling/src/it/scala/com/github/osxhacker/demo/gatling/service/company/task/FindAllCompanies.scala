package com.github.osxhacker.demo.gatling.service.company.task

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.structure.ChainBuilder

import com.github.osxhacker.demo.api.company._
import com.github.osxhacker.demo.gatling.ServiceEndpoint
import com.github.osxhacker.demo.gatling.service.company.CompanySessionKeys


/**
 * The '''FindAllCompanies''' type defines a
 * [[com.github.osxhacker.demo.gatling.service.company.task.CompanyTask]]
 * responsible for retrieving all currently defined
 * [[com.github.osxhacker.demo.api.company.Companies]], conditionally enforcing
 * that the result has at least the `minimumExisting` number of
 * [[com.github.osxhacker.demo.api.company.Company]] instances.
 */
final case class FindAllCompanies (
	private val endpoint : ServiceEndpoint,
	private val companiesKey : CompanySessionKeys
	)
	(implicit override val configuration : GatlingConfiguration)
	extends CompanyTask ()
{
	def apply (minimumExisting : Int = 0) : ChainBuilder =
		exec (logSession ("before GET:")).exec (
			http ("Find All Companies").get (endpoint / "api" / "companies")
				.addCorrelationId ()
				.check (
					isOk,
					jsonPath ("$._links").count.is (1),
					jsonPath ("$.companies").exists,
					jsonPath ("$.companies").ofType[Seq[Any]]
						.transform (_.length)
						.gte (minimumExisting)
					)
				.logResponse ()
				.mapTo[Companies] (companiesKey)
			)
			.exec (logSession ("after GET:"))
}

