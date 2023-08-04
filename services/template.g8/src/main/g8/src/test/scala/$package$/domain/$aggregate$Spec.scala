package $package$.domain

import java.time.Instant

import eu.timepit.refined
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.chassis.domain.entity._


/**
 * The '''$aggregate$Spec ''' type defines the unit-tests which certify
 * [[$package$.domain.$aggregate$]] for
 * fitness of purpose and serves as an exemplar of its use.
 */
final class $aggregate$Spec ()
	extends AnyWordSpec
		with Diagrams
		with ProjectSpec
		with $aggregate$Support
		with ScalaCheckPropertyChecks
{
	/// Class Imports
	import refined.auto._


	/// Instance Properties
	private val epoch = ModificationTimes (
		createdOn = Instant.ofEpochSecond (0L),
		lastChanged = Instant.ofEpochSecond (0L)
		)


	"The $aggregate$ entity" must {
		"define its hash code by id and version alone" in {
			forAll {
				instance : $aggregate$ =>
					val changed = $aggregate$.timestamps
						.replace (epoch) (instance)

					assert (instance.hashCode () === instance.hashCode ())
					assert (instance.hashCode () === changed.hashCode ())
				}
			}

		"be able to detect changes other than modification times" in {
			forAll {
				instance : $aggregate$ =>
					val differentStatus = instance.changeStatusTo[ErrorOr] (
						$aggregate$Status.Inactive
						)
						.orFail ()

					val changedTimestamps = $aggregate$.timestamps
						.replace (epoch) (instance)

					assert (instance.differsFrom (instance) === false)
					assert (instance.differsFrom (differentStatus) === true)
					assert (instance.differsFrom (changedTimestamps) === false)
				}
			}

		"support higher-kinded 'unless'" in {
			forAll {
				instance : $aggregate$ =>
					val populated = instance.unless (_.id.toUrn ().isEmpty) (_.id)
					val empty = instance.unless (_.id.toUrn ().nonEmpty) {
						_ => fail ("should never be evaluated")
						}

					assert (populated.isDefined)
					assert (empty.isEmpty)
					assert (
						populated.exists (_ === $aggregate$.id.get (instance))
						)
				}
			}

		"support higher-kinded 'when'" in {
			forAll {
				instance : $aggregate$ =>
					val populated = instance.when (_.id.toUrn ().nonEmpty) (
						_.version
						)

					val empty = instance.when (_.id.toUrn ().isEmpty) {
						_ => fail ("should never be evaluated")
						}

					assert (populated.isDefined)
					assert (empty.isEmpty)
					assert (
						populated.exists (_ === $aggregate$.version.get (instance))
						)
				}
			}

		"be able to 'touch' version and modification times" in {
			import $aggregate$.{
				id,
				timestamps,
				version
				}


			forAll {
				instance : $aggregate$ =>
					val touched = instance.touch[ErrorOr] ()
						.orFail ()

					assert (instance !== touched)
					assert (instance.differsFrom (touched) === true)
					assert (id.get (instance) === id.get (touched))
					assert (
						timestamps.get (instance) !== timestamps.get (touched)
						)

					assert (version.get (instance) !== version.get (touched))
				}
			}
		}
}

