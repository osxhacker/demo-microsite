package com.github.osxhacker.demo.chassis

import java.security.SecureRandom

import scala.language.postfixOps
import scala.reflect.ClassTag

import eu.timepit.refined
import org.scalacheck._
import org.scalatest.Suite


/**
 * The '''ProjectSpec''' `trait` defines a common
 * [[http://www.scalatest.org/user_guide/selecting_a_style Scalatest style]]
 * to use across unit tests in `service` projects.
 *
 * By incorporating the [[eu.timepit.refined.scalacheck]] and
 * [[org.scalacheck.ScalacheckShapeless]] types into the '''ProjectSpec'''
 * inheritance, [[org.scalacheck]] support for `scraml` generated types is made
 * available "largely for free."
 */
trait ProjectSpec
	extends ProjectScalacheckImplicits
{
	/// Self Type Constraints
	this : Suite =>


	/// Class Imports
	import cats.syntax.either._


	/// Class Types
	implicit class EitherOps[E <: Throwable, A] (
		private val self : Either[E, A]
		)
		(implicit private val CT : ClassTag[A])
	{
		/**
		 * This orFail method yields the contained ''A'' in '''self''' or will
		 * `fail` with a simple message and the [[Throwable]] contained in
		 * '''self'''.
		 */
		@inline
		def orFail () : A =
			orFail (s"unable to produce a ${CT.runtimeClass.getName}")


		/**
		 * This orFail method yields the contained ''A'' in '''self''' or will
		 * `fail` with the given '''message''' and the [[Throwable]] contained
		 * in '''self'''.
		 */
		@inline
		def orFail (message : String) : A = self valueOr {
			fail (message, _)
			}


		/**
		 * The orThrow method yields the contained ''A'' in '''self''' or
		 * `throw`s the [[Throwable]] '''self''' has.
		 */
		@inline
		def orThrow () : A = self valueOr (error => throw error)
	}


	implicit class GenOps[A] (private val self : Gen[A])
	{
		/**
		 * The toRefined method provides a declarative mechanism to "lift"
		 * ''A'' into a [[eu.timepit.refined.api.RefinedType]] by way of the
		 * "partially applied" Scala idiom.  An example of how this works is:
		 *
		 * {{{
		 *    val aRefinedGen = Gen.const ("some value")
		 *        .toRefined[Refined[String, MinSize[Witness.`3`.T]]] ()
		 * }}}
		 *
		 * Note that toRefined __explicitly does not__ have a nullary argument
		 * list.  By doing so, the ''PartiallyAppliedToRefined.apply'' method
		 * __can__ require a nullary argument list, thus allowing ''FTP'' to be
		 * deconstructed.
		 */
		@inline
		def toRefined[FTP] : PartiallyAppliedToRefined[FTP, A] =
			new PartiallyAppliedToRefined[FTP, A] (self)
	}


	implicit class OptionOps[A] (private val self : Option[A])
	{
		/**
		 * The orFail method yields the contained ''A'' in '''self''' or will
		 * `fail` with the given '''message''' if '''self''' `isEmpty`.
		 */
		@inline
		def orFail (message : String) : A = self getOrElse {
			fail (message)
			}
	}


	/**
	 * The '''Generators''' `object` provides [[org.scalacheck.Arbitrary]]
	 * instances for types which [[eu.timepit.refined.scalacheck]] cannot
	 * reasonably generate (usually due to
	 * [[https://github.com/raml-org/raml-spec/blob/master/versions/raml-10/raml-10.md/#built-in-types RAML]]
	 * property facets).
	 */
	object Generators
	{
		/// Class Imports
		import mouse.any._
		import refined.api.RefinedType


		/**
		 * The boundedNumber method creates a ''FTP'' instance having a
		 * [[scala.math.Numeric]] value within the inclusive range of '''min'''
		 * and '''max'''.
		 */
		@inline
		def boundedNumber[FTP, A] (min : A, max : A)
			(
				implicit
				choose : Gen.Choose[A],
				numeric : Numeric[A],
				rt : RefinedType[FTP]
			)
			: Gen[rt.F[A, rt.P]] =
			arbitraryRefType[rt.F, A, rt.P] {
				Gen.chooseNum[A] (min, max)
				} (rt.refType)
				.arbitrary


		/**
		 * This version of the boundedString method creates a ''FTP'' instance
		 * with a ''String'' of a length within the '''closed''' ''Range'' and
		 * consisting of any `Gen.alphaNumChar`.
		 */
		@inline
		def boundedString[FTP] (closed : Range)
			(implicit rt : RefinedType[FTP])
			: Gen[rt.F[String, rt.P]] =
			boundedString[FTP] (closed, Gen.alphaNumChar)


		/**
		 * This version of the boundedString method creates a ''FTP'' instance
		 * with a ''String'' of a length within the '''closed''' ''Range'' and
		 * consisting of the ''Char''s produced by the '''underlying'''
		 * [[org.scalacheck.Gen]].
		 */
		def boundedString[FTP] (closed : Range, underlying : Gen[Char])
			(implicit rt : RefinedType[FTP])
			: Gen[rt.F[String, rt.P]] =
			arbitraryRefType[rt.F, String, rt.P] {
				Gen.choose (closed.start, closed.end)
					.flatMap {
						length =>
							Gen.buildableOfN[String, Char] (
								length,
								underlying |> filterNewlines
								)
						}
				} (rt.refType)
				.arbitrary


		/**
		 * This version of the trimmedString method creates a ''FTP'' instance
		 * with a trimmed ''String'' of a length within the '''closed'''
		 * ''Range'' and consisting of any `Gen.alphaNumChar`.
		 */
		@inline
		def trimmedString[FTP] (closed : Range)
			(implicit rt : RefinedType[FTP])
			: Gen[rt.F[String, rt.P]] =
			trimmedString[FTP] (closed, Gen.alphaNumChar)


		/**
		 * This version of the trimmedString method creates a ''FTP'' instance
		 * with a trimmed ''String'' of a length within the '''closed'''
		 * ''Range'' and consisting of the ''Char''s produced by the
		 * '''underlying''' [[org.scalacheck.Gen]].
		 */
		def trimmedString[FTP] (closed : Range, underlying : Gen[Char])
			(implicit rt : RefinedType[FTP])
			: Gen[rt.F[String, rt.P]] =
			arbitraryRefType[rt.F, String, rt.P] {
				for {
					length <- Gen.choose (closed.start, closed.end)
					start <- Gen.stringOfN (
						1,
						underlying.filterNot (_.isWhitespace)
						)

					middle <- Gen.stringOfN (
						length - 2,
						underlying |> filterNewlines
						)

					end <- Gen.stringOfN (
						1,
						underlying.filterNot (_.isWhitespace)
						)
					} yield start + middle + end
				} (rt.refType)
				.arbitrary


		/**
		 * The urn method creates a ''FTP'' instance consisting of the prefix
		 * `"urn:`'''category'''`:` and having a random `Gen.uuid`.
		 */
		def urn[FTP] (category : String)
			(implicit rt : RefinedType[FTP])
			: Gen[rt.F[String, rt.P]] =
			arbitraryRefType[rt.F, String, rt.P] {
				Gen.uuid.map {
					uuid =>
						s"urn:$category:${uuid.toString}"
					}
				} (rt.refType)
				.arbitrary


		@inline
		private def filterNewlines (gen : Gen[Char]) : Gen[Char] =
			gen.filter ("\r\n".indexOf (_) === -1)
	}


	final class PartiallyAppliedToRefined[FTP, A] (
		private val underlying : Gen[A]
		)
	{
		/// Class Imports
		import refined.api.RefinedType


		def apply[F[_, _], P] ()
			(implicit rt : RefinedType.Aux[FTP, F, A, P])
			: Gen[F[A, P]] =
			underlying.flatMap {
				a =>
					arbitraryRefType[F, A, P] (a) (rt.refType).arbitrary
				}
	}


	/// Instance Properties
	private val randomSeed = new SecureRandom ()


	/**
	 * The createArbitrary method uses [[org.scalacheck.Arbitrary]] to
	 * instantiate an arbitrary instance of type ''A'' `orFail`s with a message
	 * indicating an ''A'' could not be generated.
	 */
	protected def createArbitrary[A] ()
		(
			implicit
			arbitrary : Arbitrary[A],
			classTag : ClassTag[A]
		)
		: A =
		arbitrary.arbitrary (
			Gen.Parameters
				.default
				.withSize (200)
				.withInitialSeed (randomSeed.nextLong ()),

			rng.Seed (randomSeed.nextLong ())
			)
			.orFail (
				s"unable to generate an arbitrary ${classTag.runtimeClass.getName}"
				)
}


sealed trait LowPriorityScalacheckImplicits
	extends refined.scalacheck.AnyInstances


/**
 * The '''ProjectScalacheckImplicits''' type brings into scope the
 * [[org.scalacheck]] and [[eu.timepit.refined.scalacheck]] library support for
 * making [[org.scalacheck.Arbitrary]] and/or [[org.scalacheck.Gen]] instances.
 *
 * While technically this allows generic creation of any ''Product'' type, in
 * practice the
 * [[https://github.com/raml-org/raml-spec/blob/master/versions/raml-10/raml-10.md/#built-in-types RAML]]
 * facets used will often result in Scalacheck giving up before being able to
 * make a conformant instance.  For those situations, explicit
 * [[org.scalacheck.Gen]]'s are defined.
 */
sealed trait ProjectScalacheckImplicits
	extends LowPriorityScalacheckImplicits
		with refined.scalacheck.BooleanInstances
		with refined.scalacheck.CharInstances
		with refined.scalacheck.GenericInstances
		with refined.scalacheck.NumericInstances
		with refined.scalacheck.NumericInstancesBinCompat1
		with refined.scalacheck.RefTypeInstances
		with refined.scalacheck.StringInstances
		with refined.scalacheck.StringInstancesBinCompat1
		with refined.scalacheck.CollectionInstances
		with refined.scalacheck.CollectionInstancesBinCompat1
		with ScalacheckShapeless

