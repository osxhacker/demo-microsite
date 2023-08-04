package com.github.osxhacker.demo.chassis.domain.repository

import cats._
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec

import com.github.osxhacker.demo.chassis.ProjectSpec


/**
 * The '''IntentSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.domain.repository.Intent]] for fitness of
 * purpose and serves as an exemplar of its use.
 */
final class IntentSpec ()
	extends AnyWordSpec
		with Diagrams
		with ProjectSpec
{
	/// Class Imports
	import cats.syntax.applicative._
	import cats.syntax.apply._
	import cats.syntax.functor._
	import cats.syntax.functorFilter._
	import cats.syntax.traverse._


	"The Intent type" must {
		"support cats Functor" in {
			val value = "hello, world!"
			val intent : Intent[String] = CreateIntent (value)
			val paired = intent.fproduct (_.length)

			assert (intent.fmap (_.length) === CreateIntent (value.length))
			assert (paired === CreateIntent (value -> value.length))
			}

		"support cats FunctorFilter" in {
			val value = 123.0
			val intent : Intent[Double] = UpsertIntent (value)
			val negative = intent.collect {
				case d if d < 0.0 =>
					d
				}

			negative match {
				case Ignore =>
					succeed

				case other =>
					fail (s"expected Ignore, but got: $other")
				}
			}

		"support flatMap" in {
			val intent : Intent[Int] = CreateIntent (99)

			intent.flatMap (UpdateIntent (_)) match {
				case UpdateIntent (x) =>
					assert (x === 99)

				case other =>
					fail (s"expected Update, but got: $other")
				}
			}
		}

	"CreateIntent" must {
		"support cats Applicative and Traverse" in {
			verifyApplicativeSupport[CreateIntent] ()
			verifyTraverseSupport[CreateIntent] ()
			}
		}

	"UpdateIntent" must {
		"support cats Applicative and Traverse" in {
			verifyApplicativeSupport[UpdateIntent] ()
			verifyTraverseSupport[UpdateIntent] ()
			}
		}

	"UpsertIntent" must {
		"support cats Applicative and Traverse" in {
			verifyApplicativeSupport[UpsertIntent] ()
			verifyTraverseSupport[UpsertIntent] ()
			}
		}


	private def verifyApplicativeSupport[F[X] <: ExecutableIntent[X]] ()
		(implicit applicative : Applicative[F])
		: Unit =
	{
		val value = "test executable intent"
		val toLength : String => Int = _.length
		val intent = value.pure[F]
		val five = intent.replicateA (3)
		val morphed = toLength.pure[F] <*> intent

		assert (five.value.length === 3)
		assert (morphed.value === value.length)
	}


	private def verifyTraverseSupport[F[X] <: ExecutableIntent[X]] ()
		(implicit applicative : Applicative[F])
		: Unit =
	{
		val value = "test traverse"
		val create : Intent[String] = value.pure[F]
		val listOfIntents = create.traverse (List.fill (3) (_))

		assert (listOfIntents.length === 3)
	}
}
