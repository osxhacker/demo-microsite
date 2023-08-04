package com.github.osxhacker.demo.chassis.domain.entity

import java.util.UUID.randomUUID

import cats.{
	Eq,
	Show
	}

import eu.timepit.refined
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import com.github.osxhacker.demo.chassis.ProjectSpec


/**
 * The '''IdentifierSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.domain.entity.Identifier]] for fitness of
 * purpose and serves as an exemplar of its use.
 */
final class IdentifierSpec ()
	extends AnyWordSpec
		with Diagrams
		with ProjectSpec
		with ScalaCheckPropertyChecks
{
	/// Class Types
	final case class Example (
		val id : Identifier[Example],
		val text : String
		)


	object Example
	{
		/// Class Imports
		import refined.auto._


		/// Implicit Conversions
		implicit val namespace = Identifier.namespaceFor[Example] ("example")
	}


	"The Identifier type" must {
		"be able to create an instance with a random UUID" in {
			val instance = Identifier.fromRandom[Example] ()

			assert (instance.belongsTo[Example] ())
			}

		"be able to create an instance with a specific UUID" in {
			val uuid = randomUUID ()
			val instance = Identifier.fromUuid[Example] (uuid)

			assert (instance.belongsTo (Example.namespace ()))
			assert (instance.toUuid () === uuid)
			}

		"be able to produce a valid URN" in {
			val instance = Identifier.fromRandom[Example] ()
			val urn = instance.toUrn ()

			assert (urn.startsWith ("urn:example:"))
			}

		"support cats Eq" in {
			assertCompiles (
				"""
	   				implicitly[Eq[Identifier[Example]]]
				"""
				)
			}

		"support cats Show" in {
			assertCompiles (
				"""
	   				implicitly[Show[Identifier[Example]]]
				"""
				)
			}
		}
}

