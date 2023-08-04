package $package$.domain.event

import io.scalaland.chimney
import monocle.macros.Lenses

import com.github.osxhacker.demo.chassis.domain.entity.Identifier
import com.github.osxhacker.demo.chassis.domain.event.{
	Region,
	ServiceFingerprint
	}

import com.github.osxhacker.demo.chassis.monitoring.CorrelationId
import $package$.domain._


/**
 * The '''CompanyProfileChanged''' type is the Domain Object Model
 * representation of an event which is emitted when a
 * [[$package$.domain.Company]] has its
 * ancillary, or "display", properties altered.
 */
@Lenses ()
final case class CompanyProfileChanged (
	override val region : Region,
	override val fingerprint : Option[ServiceFingerprint],
	override val correlationId : CorrelationId,
	override val id : Identifier[Company],
	val name : Company.Name
	)
	extends CompanyEvent

