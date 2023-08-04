package com.github.osxhacker.demo.chassis.monitoring

import java.util.UUID

import scala.util.Try

import cats.{
	Eq,
	Show
	}

import org.scalacheck.Gen
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.chassis.domain.ErrorOr


/**
 * The '''CorrelationIdSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.monitoring.CorrelationId]] for fitness of
 * purpose and serves as an exemplar of its use.
 */
final class CorrelationIdSpec ()
	extends AnyWordSpec
		with Diagrams
		with ProjectSpec
		with ScalaCheckPropertyChecks
{
	"The CorrelationId type" must {
		"support strings containing UUID v3+ canonical representations" in {
			val valid = Gen.uuid
				.map (_.toString)

			forAll (valid) {
				candidate =>
					val result = CorrelationId[ErrorOr] (candidate)

					assert (result.isRight)
				}
			}

		"support v3+ UUID instances" in {
			val valid = Gen.uuid

			forAll (valid) {
				candidate =>
					val result = CorrelationId[ErrorOr] (candidate)

					assert (result.isRight)
				}
			}

		"disallow the 'nil' UUID" in {
			val nil = UUID.fromString ("00000000-0000-0000-0000-000000000000")
			val result = CorrelationId[Try] (nil)

			assert (result.isFailure)
			}

		"disallow v1 representations" in {
			val candidate = "98712c2c-522f-11ed-bd63-000000000000"
			val v1 = Try (UUID.fromString (candidate))
			val result = CorrelationId[ErrorOr] (candidate)

			assert (v1.isSuccess)
			assert (result.isLeft)
			}

		"disallow v2 representations" in {
			val candidate = "98712c2c-522f-21ed-bd63-000000000000"
			val v2 = Try (UUID.fromString (candidate))
			val result = CorrelationId[ErrorOr] (candidate)

			assert (v2.isSuccess)
			assert (result.isLeft)
			}

		"support cats Eq" in {
			assertCompiles (
				"""
	   				implicitly[Eq[CorrelationId]]
				"""
				)
			}

		"support cats Show" in {
			assertCompiles (
				"""
	   				implicitly[Show[CorrelationId]]
				"""
				)
			}
		}
}

