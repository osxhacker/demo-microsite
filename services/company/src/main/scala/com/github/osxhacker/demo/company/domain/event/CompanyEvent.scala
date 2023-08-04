package com.github.osxhacker.demo.company.domain.event

import com.github.osxhacker.demo.chassis.domain.entity.Identifier
import com.github.osxhacker.demo.chassis.domain.event.{
	Region,
	ServiceFingerprint
	}

import com.github.osxhacker.demo.chassis.monitoring.CorrelationId
import com.github.osxhacker.demo.company.domain.Company


/**
 * The '''CompanyEvent''' `trait` defines the Domain Object Model common
 * ancestor to __all__ [[com.github.osxhacker.demo.company.domain.Company]]
 * domain events known to the company microservice.
 */
trait CompanyEvent
{
	/// Instance Properties
	val region : Region
	val fingerprint : Option[ServiceFingerprint]
	val correlationId : CorrelationId
	val id : Identifier[Company]
}

