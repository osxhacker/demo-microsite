package com.github.osxhacker.demo.company.adapter.kafka.specification

import com.github.osxhacker.demo.chassis.domain.Specification
import com.github.osxhacker.demo.chassis.domain.event.ServiceFingerprint
import com.github.osxhacker.demo.company.adapter.rest.api


/**
 * The '''DifferentService''' type is the `adapter`
 * [[com.github.osxhacker.demo.chassis.domain.Specification]] responsible for
 * determining if an arbitrary
 * [[com.github.osxhacker.demo.company.adapter.rest.api.CompanyEvent]]
 * originates from a microservice __other than__ the one identified by a known
 * `fingerprint`.
 */
final case class DifferentService (private val fingerprint : ServiceFingerprint)
	extends Specification[api.CompanyEvent]
{
	/// Class Imports
	import cats.syntax.eq._
	import cats.syntax.show._


	/// Instance Properties
	private val thisService = fingerprint.show


	override def apply (candidate : api.CompanyEvent) : Boolean =
		candidate.origin
			.fingerprint
			.exists (_.value =!= thisService)
}

