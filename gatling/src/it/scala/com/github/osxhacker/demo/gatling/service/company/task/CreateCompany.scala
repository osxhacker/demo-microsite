package com.github.osxhacker.demo.gatling.service.company.task

import java.util.UUID.randomUUID

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.structure.ChainBuilder

import com.github.osxhacker.demo.api.company._
import com.github.osxhacker.demo.gatling.ServiceEndpoint
import com.github.osxhacker.demo.gatling.service.company.CompanySessionKeys


/**
 * The '''CreateCompany''' type defines the
 * [[com.github.osxhacker.demo.gatling.service.company.task.CompanyTask]]
 * responsible for creating a new
 * [[com.github.osxhacker.demo.api.company.Company]].  On successful completion,
 * the [[io.gatling.core.session.Session]] will have the generated slug in
 * [[com.github.osxhacker.demo.gatling.service.company.CompanySessionKeys.GeneratedSlugEntry]]
 * and the newly created [[com.github.osxhacker.demo.api.company.Company]] in
 * [[com.github.osxhacker.demo.gatling.service.company.CompanySessionKeys.TargetCompanyEntry]].
 */
final case class CreateCompany (
	private val endpoint : ServiceEndpoint
	)
	(implicit override val configuration : GatlingConfiguration)
	extends CompanyTask ()
{
	/// Instance Properties
	private val locationEntry = "tmp-location"


	def apply (slug : String, status : CompanyStatus) : ChainBuilder = exec (
		http ("Create New Company").post (endpoint / "api" / "companies")
			.addCorrelationId ()
			.body (
				NewCompany (
					slug = slug,
					name = s"Gatling ${randomUUID ()}",
					status = status,
					description = "Added by Gatling..."
					)
				)
			.check (
				isCreated,
				header (HttpHeaderNames.Location).exists
					.saveAs (locationEntry)
				)
			.logResponse ()
		)
		.exitHereIfFailed
		.exec {
			CompanySessionKeys.GeneratedSlugEntry
				.session
				.replace (slug)
			}
		.exec (
			http ("Retrieve Created Company").get {
				_ (locationEntry).validate[String]
					.map (endpoint / _)
				}
				.addCorrelationId ()
				.logResponse ()
				.check (isOk)
				.mapTo[Company] (CompanySessionKeys.TargetCompanyEntry)
			)
		.exec (_.remove (locationEntry))
		.exitHereIfFailed
}

