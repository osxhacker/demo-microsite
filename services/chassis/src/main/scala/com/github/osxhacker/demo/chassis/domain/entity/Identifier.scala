package com.github.osxhacker.demo.chassis.domain.entity

import java.util.UUID

import scala.util.{
	Success,
	Try
	}

import cats.{
	ApplicativeThrow,
	Eq,
	Show
	}

import cats.data.Kleisli
import com.softwaremill.diffx
import com.softwaremill.diffx.Diff
import eu.timepit.refined


/**
 * The '''Identifier''' type defines the concept of a unique domain identifier
 * in terms of [[https://www.rfc-editor.org/rfc/rfc8141 URN]] concepts, where
 * `nid` categorizes a domain entity and `nss` is a [[java.util.UUID]].
 */
final case class Identifier[EntityT] (
	private val nid : Identifier.Namespace,
	private val nss : UUID
	)
{
	/// Class Imports
	import cats.syntax.eq._
	import refined.cats._


	override def toString () : String = toUrn ()


	/**
	 * This version of the belongsTo method determines if '''this''' instance
	 * has the same
	 * [[com.github.osxhacker.demo.chassis.domain.entity.Identifier.Namespace]]
	 * the '''reslover''' provides associated with ''EntityT''.
	 */
	def belongsTo[A] ()
		(implicit resolver : Identifier.EntityNamespace[A])
		: Boolean =
		belongsTo (resolver ())


	/**
	 * This version of the belongsTo method determines if '''this''' instance
	 * has the same given '''namespace'''.
	 */
	def belongsTo (namespace : Identifier.Namespace) : Boolean =
		nid === namespace


	/**
	 * The toUrn method creates a fully qualified
	 * [[https://www.rfc-editor.org/rfc/rfc8141 URN]] from '''this''' instance.
	 */
	def toUrn () : String = s"urn:$nid:$nss"


	/**
	 * The toUuid method produces a [[java.util.UUID]] from `nss`.  How `nss`
	 * becomes a [[java.util.UUID]] is implementation-defined.  This method will
	 * __always__ succeed assuming sufficient memory is available.
	 */
	def toUuid () : UUID = nss
}


object Identifier
{
	/// Class Imports
	import UUID.randomUUID
	import cats.syntax.either._
	import cats.syntax.eq._
	import diffx.refined._
	import refined.api.Refined

	import refined.boolean.{
		And,
		Or
		}

	import refined.char.LowerCase
	import refined.collection.{
		Forall,
		NonEmpty,
		Size
		}

	import refined.generic.Equal
	import refined.numeric.Interval
	import refined.string.Trimmed


	/// Class Types
	/**
	 * The '''Namespace''' type is defined to conform with the
	 * [[https://www.rfc-editor.org/rfc/rfc8141#page-10 URN NID specification]].
	 * It is possible to make nonsensical instances by design, however most
	 * mistakes will be caught with this definition.
	 */
	type Namespace = Refined[
		String,
		Trimmed
			And Size[Interval.Closed[1, 30]]
			And Forall[LowerCase Or Equal['-']]
		]


	/**
	 * The '''EntityNamespace''' type is a model of the TYPE CLASS pattern and
	 * defines the contract for resolving what ''Namespace'' is associated with
	 * an ''EntityT''.
	 */
	sealed trait EntityNamespace[EntityT]
	{
		def apply () : Namespace
	}


	/**
	 * The '''Parser''' type is a model of the TYPE CLASS pattern and defines
	 * the contract for internalizing an '''Identifier''' based on a `candidate`
	 * ''ValueT''.
	 */
	sealed trait Parser[EntityT, ValueT]
		extends (ValueT => Try[Identifier[EntityT]])


	object Parser
	{
		/// Class Imports
		import cats.syntax.applicative._


		/// Implicit Conversions
		implicit def parserForIdentity[EntityT]
			: Parser[EntityT, Identifier[EntityT]] =
			new Parser[EntityT, Identifier[EntityT]] {
				override def apply (candidate : Identifier[EntityT])
					: Try[Identifier[EntityT]] =
					Success (candidate)
				}


		implicit def parserForRefined[EntityT, P] (
			implicit namespace : EntityNamespace[EntityT]
			)
			: Parser[EntityT, Refined[String, P]] =
			new Parser[EntityT, Refined[String, P]] {
				override def apply (candidate : Refined[String, P])
					: Try[Identifier[EntityT]] =
					Try (UUID.fromString (candidate.value))
						.map (fromUuid[EntityT])
						.orElse (fromUrn[Try, EntityT] (candidate))
				}


		implicit def parserForString[EntityT] (
			implicit namespace : EntityNamespace[EntityT]
			)
			: Parser[EntityT, String] =
			new Parser[EntityT, String] {
				/// Class Imports
				import refined.api.RefType.applyRef

				/// Class Types
				private type MinimallyRefined = NonEmpty And Trimmed
				private type CandidateType = Refined[String, MinimallyRefined]


				/// Instance Properties
				private val logic =
					Kleisli[Try, String, CandidateType] (
						applyRef[CandidateType] (_)
							.leftMap (new IllegalArgumentException (_))
							.toTry
						).flatMapF (
							parserForRefined[EntityT, MinimallyRefined].apply
							)

				override def apply (candidate : String)
					: Try[Identifier[EntityT]] =
					logic (candidate)
				}


		implicit def parserForUuid[EntityT] (
			implicit namespace : EntityNamespace[EntityT]
			)
			: Parser[EntityT, UUID] =
			new Parser[EntityT, UUID] {
				override def apply (candidate : UUID)
					: Try[Identifier[EntityT]] =
					fromUuid[EntityT] (candidate).pure[Try]
				}
	}


	/**
	 * The fromRandom method creates an '''Identifier''' belonging to the
	 * ''EntityT'' with a `randomUUID`.
	 */
	def fromRandom[EntityT] ()
		(implicit resolver : Identifier.EntityNamespace[EntityT])
		: Identifier[EntityT] =
		fromUuid[EntityT] (randomUUID)


	/**
	 * The fromUrn method attempts to create an '''Identifier''' from a
	 * [[eu.timepit.refined.api.Refined]] '''urn''' and producing errors within
	 * ''F'' it it is malformed.
	 */
	def fromUrn[F[_], EntityT] (urn : Refined[String, _])
		(
			implicit
			applicativeThrow : ApplicativeThrow[F],
			resolver : Identifier.EntityNamespace[EntityT]
		)
		: F[Identifier[EntityT]] =
		urn.value.split (':') match {
			case Array ("urn", nid, nss) if resolver ().value === nid =>
				Try (UUID.fromString (nss)).map (fromUuid[EntityT])
					.toEither
					.liftTo[F]

			case Array ("urn", nid, _) =>
				applicativeThrow.raiseError (
					new IllegalArgumentException (
						s"invalid identifier namespace: '$nid'"
						)
					)

			case _ =>
				applicativeThrow.raiseError (
					new IllegalArgumentException (
						s"invalid urn: '${urn.value}'"
						)
					)
			}


	/**
	 * The fromUuid method creates an '''Identifier''' belonging to the
	 * specified ''EntityT'' and having the given '''uuid'''.
	 */
	def fromUuid[EntityT] (uuid : UUID)
		(implicit resolver : Identifier.EntityNamespace[EntityT])
		: Identifier[EntityT] =
		new Identifier[EntityT] (resolver (), uuid)


	/**
	 * The namespaceFor method is a helper for defining
	 * [[com.github.osxhacker.demo.chassis.domain.entity.Identifier.Namespace]]s
	 * for arbitrary ''EntityT''.  Typical usage is:
	 *
	 * {{{
	 * final case class Example (...)
	 *
	 * object Example
	 * {
	 *     import eu.timepit.refined.auto._
	 *
	 *     implicit val namespace = Identifier.namespaceFor[Example] ("example")
	 * }
	 * }}}
	 */
	def namespaceFor[EntityT] (namespace : Namespace)
		: EntityNamespace[EntityT] =
		new EntityNamespace[EntityT] {
			override def apply () : Namespace = namespace
			}


	/// Implicit Conversions
	implicit def identifierDiff[EntityT] : Diff[Identifier[EntityT]] =
		Diff.derived[Identifier[EntityT]]

	implicit def identifierEq[EntityT] : Eq[Identifier[EntityT]] =
		Eq.fromUniversalEquals

	implicit def identifierShow[EntityT] : Show[Identifier[EntityT]] =
		Show.show (_.toUrn ())
}

