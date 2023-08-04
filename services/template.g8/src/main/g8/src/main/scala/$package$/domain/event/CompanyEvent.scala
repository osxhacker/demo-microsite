package $package$.domain.event

import monocle.Getter

import com.github.osxhacker.demo.chassis.domain.entity.Identifier
import com.github.osxhacker.demo.chassis.domain.event.{
	Region,
	ServiceFingerprint
	}

import com.github.osxhacker.demo.chassis.monitoring.CorrelationId
import $package$.domain.Company


/**
 * The '''CompanyEvent''' `trait` defines the Domain Object Model common
 * ancestor to __all__
 * [[$package$.domain.Company]] domain events
 * known to the storage-facility microservice.
 */
trait CompanyEvent
{
	/// Instance Properties
	val region : Region
	val fingerprint : Option[ServiceFingerprint]
	val correlationId : CorrelationId
	val id : Identifier[Company]
}


object CompanyEvent
{
	/// Instance Properties
	val correlationId = Getter[CompanyEvent, CorrelationId] (_.correlationId)
	val fingerprint = Getter[CompanyEvent, Option[ServiceFingerprint]] (
		_.fingerprint
		)

	val id = Getter[CompanyEvent, Identifier[Company]] (_.id)
	val region = Getter[CompanyEvent, Region] (_.region)
}

