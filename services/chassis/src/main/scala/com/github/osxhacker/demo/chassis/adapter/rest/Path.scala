package com.github.osxhacker.demo.chassis.adapter.rest

import java.net.URI

import scala.annotation.tailrec
import scala.language.{
	implicitConversions,
	postfixOps
	}

import cats._
import eu.timepit.refined
import eu.timepit.refined.api.{
	RefinedType,
	Refined
	}

import sttp.model.Uri

import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.chassis.domain.error.DomainValueError


/**
 * The '''Path''' type is a
 * [[https://docs.scala-lang.org/overviews/core/value-classes.html value class]]
 * which reifies the concept of a logical
 * [[https://www.rfc-editor.org/rfc/rfc3986#section-3.3 URI path]].
 */
sealed class Path private (private val location : String)
	extends Product1[String]
		with Serializable
{
	/// Class Imports
	import cats.syntax.eq._
	import mouse.boolean._
	import mouse.option._


	/// Instance Properties
	final override val _1 : String = location
	final override val productPrefix : String = "Path"

	lazy val parent = new Path (
		location.stripPrefix ("/")
			.split ('/')
			.init
			.mkString ("/", "/", "")
		)


	final override def canEqual (that : Any) : Boolean = that.isInstanceOf[Path]


	final override def equals (obj : Any) : Boolean =
		canEqual (obj) && location.equals (obj.asInstanceOf[Path].location)


	final override def hashCode () : Int = location.hashCode ()


	final override def productElementName (n : Int) : String =
		(n === 0).fold ("location", super.productElementName (n))


	final override def toString () = s"$productPrefix($location)"


	/**
	 * The combine method produces a '''Path''' having '''this''' `content` and
	 * the '''other''' as one logical '''Path'''.
	 */
	def combine (other : Path) : Path =
		other.asSubPath ()
			.map (location + _)
			.cata (new Path (_), this)


	/**
	 * This version of the toUri method creates an [[java.net.URI]] with only
	 * a '''path''' component.
	 */
	def toUri () : URI = new URI (location)


	/**
	 * This version of the toUri method creates an [[java.net.URI]] by using the
	 * given '''template''' and replacing __only__ its '''path''' component.
	 */
	def toUri (template : URI) : URI =
		new URI (
			template.getScheme,
			template.getUserInfo,
			template.getHost,
			template.getPort,
			location,
			template.getQuery,
			template.getFragment
			)


	protected def asSubPath () : Option[String] = Some (location)
}


object Path
{
	/// Class Imports
	import cats.syntax.either._
	import cats.syntax.eq._
	import cats.syntax.option._
	import mouse.boolean._
	import refined.boolean.{
		And,
		Or
		}

	import refined.collection.NonEmpty
	import refined.generic.Equal
	import refined.string.{
		MatchesRegex,
		Trimmed
		}


	/// Class Types
	type PredicateType = Trimmed And
		NonEmpty And
		Or[
			Equal["/"],
			MatchesRegex["^(?:/[A-Za-z0-9][A-Za-z0-9._-]*)+$"]
			]


	/**
	 * The '''Parser''' type is a model of the TYPE CLASS pattern and defines
	 * the contract for internalizing a '''Path''' based on a supported
	 * ''ValueT''.
	 */
	sealed trait Parser[ValueT]
		extends (ValueT => ErrorOr[Path])


	object Parser
	{
		/// Class Imports
		import cats.syntax.either._


		/// Implicit Conversions
		implicit val parserForIdentity : Parser[Path] =
			new Parser[Path] {
				override def apply (path : Path) : ErrorOr[Path] = path.asRight
				}

		implicit lazy val parserForRefinedString : Parser[Refined[String, _]] =
			new Parser[Refined[String, _]] {
				override def apply (candidate : Refined[String, _])
					: ErrorOr[Path] =
					parserForString (candidate.value)
				}

		implicit lazy val parserForString : Parser[String] =
			new Parser[String] {
				override def apply (candidate : String) : ErrorOr[Path] =
					RefinedType[Refined[String, PredicateType]].refine (
						candidate
						)
						.bimap (
							DomainValueError (_),
							path => Path (path)
							)
				}

		implicit lazy val parserForUri : Parser[Uri] =
			new Parser[Uri] {
				override def apply (candidate : Uri) : ErrorOr[Path] =
					parserForString (
						Uri.AbsolutePath (
							candidate.pathSegments
								.segments
								.filterNot (_.v.isEmpty)
								.toSeq
							)
							.toString ()
						)
			}
	}


	/// Instance Properties
	/**
	 * The '''Root''' instance defines an `empty`, valid,
	 * [[com.github.osxhacker.demo.chassis.adapter.rest.Path]] capable of
	 * satisfying the [[cats.Monoid]] laws.
	 */
	val Root : Path = new Path ("/") {
		/// Instance Properties
		override lazy val parent : Path = this


		override def combine (other : Path) : Path = other


		override protected def asSubPath () : Option[String] = None
		}


	/**
	 * The apply method is provided to support functional-style creation of a
	 * ''Path'' instance.  When the given '''instance''' is `/`, the resultant
	 * ''Path'' is guaranteed to be '''Root'''.
	 */
	def apply (instance : Refined[String, PredicateType]) : Path =
		(instance.value === "/").fold (Root, new Path (instance.value))


	/**
	 * The from method attempts to create a '''Path''' with an arbitrary
	 * '''instance''' within the container ''F''.
	 */
	def from[F[_], A] (instance : A)
		(
			implicit
			applicativeThrow : ApplicativeThrow[F],
			parser : Parser[A]
		)
		: F[Path] =
		parser (instance).liftTo[F]


	/**
	 * The unapply method is provided to support pattern matching on supported
	 * '''candidate''' types in attempt to produce a '''Path'''.
	 */
	@tailrec
	def unapply (obj : Any) : Option[Path] =
		obj match {
			case path : Path =>
				path.some

			case Right (any) =>
				unapply (any)

			case Refined (any) =>
				unapply (any)

			case string : String =>
				RefinedType[Refined[String, PredicateType]].refine (string)
					.map (apply)
					.toOption

			case _ =>
				none[Path]
			}


	/// Implicit Conversions
	implicit val pathEq : Eq[Path] = Eq.by (_.location)
	implicit val pathMonoid : Monoid[Path] =
		new Monoid[Path] {
			/// Instance Properties
			override val empty : Path = Root


			override def combine (lhs : Path, rhs : Path) : Path =
				lhs combine rhs
			}

	implicit val pathShow : Show[Path] = Show.show (_.location)
}

