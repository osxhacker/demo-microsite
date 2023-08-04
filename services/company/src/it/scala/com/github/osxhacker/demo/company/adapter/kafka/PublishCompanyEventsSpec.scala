package com.github.osxhacker.demo.company.adapter.kafka

import cats.effect.IO

import com.github.osxhacker.demo.chassis
import com.github.osxhacker.demo.company.domain.event._


/**
 * The '''PublishCompanyEventsSpec''' type defines the integration-tests which
 * certify
 * [[com.github.osxhacker.demo.company.adapter.kafka.PublishCompanyEvents]] for
 * fitness of purpose and serves as an exemplar of its use.
 */
final class PublishCompanyEventsSpec ()
	extends IntegrationSpec (IntegrationSettings.PublishCompanyEvents)
{
	/// Class Imports
	import chassis.syntax._


	"The PublishCompanyEvents type" must {
		"be able to emit a 'CompanyCreated' integration event" in withScopedEnvironment {
			implicit env =>
				IO.unit
					.addEvent (CompanyCreated (createCompany ()))
					.broadcast ()
					.as (succeed)
			}

		"be able to emit a 'CompanyDeleted' integration event" in withScopedEnvironment {
			implicit env =>
				IO.unit
					.addEvent (CompanyDeleted (createCompany ()))
					.broadcast ()
					.as (succeed)
			}
		}
}

