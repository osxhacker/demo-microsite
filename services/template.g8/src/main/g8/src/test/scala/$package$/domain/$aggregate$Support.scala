package $package$.domain

import scala.language.{
	implicitConversions,
	postfixOps
	}

import org.scalacheck.{
	Arbitrary,
	Gen
	}

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.chassis.domain.entity._


/**
 * The '''$aggregate$Support''' type defines common behaviour for
 * generating
 * [[$package$.domain.$aggregate$]] types.
 * By providing `implicit` [[org.scalacheck.Arbitrary]] methods for
 * '''$aggregate$''' types having very specific
 * [[eu.timepit.refined.api.Refined]] types, generating instances has a
 * __much__ higher probability of success.  Without them, it would be virtually
 * impossible to create a valid URN for example.
 */
trait $aggregate$Support
{
	/// Self Type Constraints
	this : ProjectSpec =>


	/// Instance Properties
	implicit protected val domain$aggregate$Status =
		Gen.const[$aggregate$Status] ($aggregate$Status.Active)

	implicit protected val domainVersion =
		Gen.posNum[Int]
			.suchThat (_ < Int.MaxValue)
			.flatMap {
				value =>
					Version[ErrorOr] (value).fold (_ => Gen.fail, Gen.const)
				}


	/// Implicit Conversions
	implicit protected def arbDomain$aggregate$ (
		implicit
		status : Gen[$aggregate$Status],
		version : Gen[Version]
		)
		: Arbitrary[$aggregate$] =
	{
		val generator = for {
			anId <- Gen.const (Identifier.fromRandom[$aggregate$] ())
			theVersion <- version
			status <- status
		} yield $aggregate$ (
				id = anId,
				version = theVersion,
				status = status,
				timestamps = ModificationTimes.now ()
				)

		Arbitrary (generator)
	}
}

