package com.github.osxhacker.demo.company.adapter

import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec
import pureconfig.ConfigSource


/**
 * The '''RuntimeSettingsSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.company.adapter.RuntimeSettings]] for
 * fitness of purpose and serves as an exemplar of its use.
 */
final class RuntimeSettingsSpec ()
	extends AnyWordSpec
		with Diagrams
{
	/// Class Types
	type SettingsError[A] = Either[Throwable, A]


	"The RuntimeSettings configuration type" must {
		"be able to load the default server configuration" in {
			assert (RuntimeSettings[SettingsError] ().isRight)
			}

		"fail when given an unknown configuration resource" in {
			val unknown = ConfigSource.resources ("does-not-exist.conf")

			assert (RuntimeSettings[SettingsError] (unknown).isLeft)
			}

		"fail when missing required sections" in {
			val result = RuntimeSettings[SettingsError] (
				ConfigSource.resources ("missing-http.conf")
				)

			assert (result.isLeft)
			assert (
				result.left.exists (_.getMessage.contains ("Key not found: 'http'"))
				)
			}

		"fail when 'idle-timeout' is not within the supported range" in {
			val result = RuntimeSettings[SettingsError] (
				ConfigSource.resources (
					"invalid-idle-timeout.conf"
					)
				)

			assert (result.isLeft)
			assert (
				result.left.exists (_.getMessage.contains ("Predicate failed"))
				)
			}
		}
}

