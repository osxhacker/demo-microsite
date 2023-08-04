package com.github.osxhacker.demo.storageFacility.domain.scenario

import monocle.macros.GenLens
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec
import squants.space.{
	CubicMeters,
	Volume
	}


/**
 * The '''ValidateStorageVolumeSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.storageFacility.domain.scenario.ValidateStorageVolume]]
 * for fitness of purpose and serves as an exemplar of its use.
 */
final class ValidateStorageVolumeSpec ()
	extends AnyWordSpec
		with Diagrams
{
	/// Class Types
	final case class SampleVolumes (
		val available : Volume,
		val capacity : Volume
		)


	object SampleVolumes
	{
		/// Instance Properties
		val available = GenLens[SampleVolumes] (_.available).asGetter
		val capacity = GenLens[SampleVolumes] (_.capacity).asGetter
	}


	"The ValidateStorageVolume algorithm" must {
		"detect when available and capacity are allowed" in {
			val result = ValidateStorageVolume (
				SampleVolumes (CubicMeters (10), CubicMeters (10)),
				SampleVolumes.available,
				SampleVolumes.capacity
				)

			assert (result.isValid)
			assert (result.exists (caa => caa.available <= caa.capacity))
			}

		"detect when available exceeds capacity" in {
			val result = ValidateStorageVolume (
				SampleVolumes (CubicMeters (100), CubicMeters (10)),
				SampleVolumes.available,
				SampleVolumes.capacity
				)

			assert (result.isInvalid)
			}

		"disallow negative available volume" in {
			val result = ValidateStorageVolume (
				SampleVolumes (-CubicMeters (10), CubicMeters (10)),
				SampleVolumes.available,
				SampleVolumes.capacity
				)

			assert (result.isInvalid)
			}

		"disallow negative capacity volume" in {
			val result = ValidateStorageVolume (
				SampleVolumes (CubicMeters (10), CubicMeters (-10)),
				SampleVolumes.available,
				SampleVolumes.capacity
				)

			assert (result.isInvalid)
			}
		}
}

