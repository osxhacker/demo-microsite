package com.github.osxhacker.demo.gatling.service.storageFacility.task

import monocle.Optional
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.{
	Expression,
	Session
	}

import com.github.osxhacker.demo.api
import com.github.osxhacker.demo.gatling.{
	ServiceEndpoint,
	Task
	}

import com.github.osxhacker.demo.gatling.service.company.{
	ResourceOptics => CompanyResourceOptics
	}

import com.github.osxhacker.demo.gatling.service.storageFacility.{
	ResourceOptics,
	StorageFacilitySessionKeys
	}


/**
 * The '''StorageFacilityTask''' type defines the common ancestor to __all__
 * [[com.github.osxhacker.demo.api.storageFacility]]-based
 * [[https://gatling.io/docs/gatling/tutorials/quickstart/ Gatling]]
 * [[com.github.osxhacker.demo.gatling.Task]]s.  Note that the `configuration`
 * property __must__ be available for use by [[io.gatling.core.CoreDsl]].
 */
abstract class StorageFacilityTask ()
	(implicit override val configuration: GatlingConfiguration)
	extends Task[StorageFacilitySessionKeys]
		with ResourceOptics
{
	/// Class Imports
	import io.gatling.commons.validation._


	/**
	 * The storageFacilitiesLocation method creates an
	 * [[io.gatling.core.session.Expression]] which results in the
	 * [[https://www.rfc-editor.org/rfc/rfc3986 URI]] for storage facilities
	 * owned by the given '''owner'''.
	 */
	protected def storageFacilitiesLocation (
		endpoint : ServiceEndpoint,
		owner : Optional[Session, api.company.Company]
		)
		: Expression[String] =
		owner.andThen (CompanyResourceOptics.company.slug)
			.headOption (_)
			.toValidation ("unable to resolve owning company slug")
			.map (endpoint / "api" / _ / "storage-facilities")
}

