package com.github.osxhacker.demo.gatling.service.company.task

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.structure.ChainBuilder

import com.github.osxhacker.demo.api
import com.github.osxhacker.demo.gatling.{
	ServiceEndpoint,
	SessionKey
	}


/**
 * The '''DeleteCompany''' type defines the
 * [[com.github.osxhacker.demo.gatling.service.company.task.CompanyTask]]
 * responsible for deleting an existing
 * [[com.github.osxhacker.demo.api.company.Company]].
 */
final case class DeleteCompany (private val endpoint : ServiceEndpoint)
	(
		implicit

		/// Needed for CompanyTask.
		override val configuration : GatlingConfiguration
	)
	extends CompanyTask ()
{
	/// Class Imports
	import io.gatling.commons.validation._


	def apply[
		KeyT <: SessionKey with SessionKey.Definition[_, api.company.Company]
		] (key : KeyT)
		: ChainBuilder =
		exec {
			http ("Delete Gatling Company").delete {
				key.session
					.to (hrefFor (company.delete))
					.some
					.headOption (_)
					.map (path => (endpoint / path).toString)
					.toValidation ("could not find 'delete' semantic link")
				}
				.addCorrelationId ()
				.check (isOk)
				.logResponse ()
			}
}

