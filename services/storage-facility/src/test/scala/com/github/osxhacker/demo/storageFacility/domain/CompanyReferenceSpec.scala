package com.github.osxhacker.demo.storageFacility.domain

import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec

import com.github.osxhacker.demo.chassis.ProjectSpec
import com.github.osxhacker.demo.chassis.domain.{
	ErrorOr,
	Slug
	}

import com.github.osxhacker.demo.chassis.domain.entity.Identifier


/**
 * The '''CompanyReferenceSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.storageFacility.domain.CompanyReference]] for
 * fitness of purpose and serves as an exemplar of its use.
 */
final class CompanyReferenceSpec ()
	extends AnyWordSpec
		with Diagrams
		with ProjectSpec
{
	/// Class Imports
	import cats.syntax.show._


	/// Instance Properties
	val testId = Identifier.fromRandom[Company] ()
	val testSlug = Slug[ErrorOr] ("a-test-slug").orFail ("unable to make a slug")


	"The CompanyReference value type" must {
		val idOnly = CompanyReference (testId)
		val slugOnly = CompanyReference (testSlug)
		val both = CompanyReference (testSlug, testId)

		assert (CompanyReference.slug.getOption (idOnly).isEmpty)
		assert (CompanyReference.slug.getOption (slugOnly).isDefined)
		assert (CompanyReference.id.getOption (slugOnly).isEmpty)
		assert (CompanyReference.slug.getOption (both).isDefined)
		assert (CompanyReference.id.getOption (both).isDefined)

		"support Slug-based equality" in {
			assert (slugOnly === CompanyReference (testSlug))
			assert (slugOnly === both)
			assert (both === slugOnly)
			assert (slugOnly !== idOnly)
			}

		"support Identifier-based equality" in {
			assert (idOnly === CompanyReference (testId))
			assert (idOnly === both)
			assert (both === idOnly)
			assert (idOnly !== slugOnly)
			}

		"support Slug-based hash codes" in {
			assert (idOnly.hashCode () === testId.hashCode ())
			assert (slugOnly.hashCode () === testSlug.hashCode ())
			assert (both.hashCode () === testId.hashCode ())
			}

		"support folding over the reference content" in {
			assert (idOnly.fold (_.show, _.show) === testId.show)
			assert (slugOnly.fold (_.show, _.show) === testSlug.show)
			assert (both.fold (_.show, _.show) === testId.show)
			}

		"have a lens which supports manipulating identifiers" in {
			val idAdded = CompanyReference.id
				.asSetter
				.replace (testId) (slugOnly)

			assert (idAdded === slugOnly)
			assert (idAdded === both)
			}

		"have a lens which supports manipulating slugs" in {
			val slugAdded = CompanyReference.slug
				.asSetter
				.replace (testSlug) (idOnly)

			assert (slugAdded === idOnly)
			assert (slugAdded === both)
			}
		}
}

