package com.github.osxhacker.demo.gatling

import scala.language.postfixOps
import scala.reflect.ClassTag

import enumeratum.EnumEntry
import io.gatling.commons.util.TypeCaster
import io.gatling.core.session.Session
import monocle.Optional


/**
 * The '''SessionKey''' type defines the contract for specifying unique
 * [[io.gatling.core.session.Session]] keys available for any given service.
 */
trait SessionKey
	extends EnumEntry
		with EnumEntry.Hyphencase
{
	/// Class Types
	type ValueType


	/// Instance Properties
	/**
	 * The session property defines the contract for managing the ''ValueType''
	 * associated with '''this''' concrete
	 * [[com.github.osxhacker.demo.gatling.SessionKey]] instance.
	 */
	def session : Optional[Session, ValueType]


	/**
	 * The convert method determines if an arbitrary '''instance''' can be
	 * retained in '''this''' [[com.github.osxhacker.demo.gatling.SessionKey]]
	 * as a ''ValueType''.  If not, a suitable error is produced.
	 */
	def convert[A <: AnyRef] (instance : A)
		(implicit classTag : ClassTag[A])
		: Either[Exception, ValueType]
}


object SessionKey
{
	/// Class Types
	/**
	 * The '''Definition''' type defines common
	 * [[com.github.osxhacker.demo.gatling.SessionKey]] functionality for
	 * specific ''KeyT'' types.  For example:
	 *
	 * {{{
	 *     sealed trait MyKeys
	 *         extends SessionKey
	 *
	 *     object MyKeys
	 *     {
	 *         case object AuthTokenEntry
	 *             extends Session.Definition[MyKeys, String]
	 *                 with MyKeys
	 *     }
	 * }}}
	 */
	abstract class Definition[KeyT <: SessionKey, ValueT] ()
		(
			implicit

			/// Needed for `asOption`
			classTag : ClassTag[ValueT],

			/// Needed for `asOption`
			typeCaster : TypeCaster[ValueT]
		)
	{
		/// Self Type Constraints
		this : KeyT =>


		/// Class Types
		final override type ValueType = ValueT


		/// Instance Properties
		final override val session : Optional[Session, ValueType] =
			Optional[Session, ValueType] (_ (entryName).asOption[ValueType]) {
				v => _.set (entryName, v)
				}


		final override def convert[A <: AnyRef] (instance : A)
			(implicit instanceClassTag : ClassTag[A])
			: Either[Exception, ValueType] =
			Either.cond (
				classTag.runtimeClass.isAssignableFrom (
					instanceClassTag.runtimeClass
					),

				instance.asInstanceOf[ValueType],
				new IllegalArgumentException (
					new StringBuilder ()
						.append (getClass.getName)
						.append (": could not convert '")
						.append (instanceClassTag.runtimeClass.getName)
						.append ("' into a '")
						.append (classTag.runtimeClass.getName)
						.append ('\'')
						.toString ()
					)
				)


		/**
		 * The add method provides syntactic sugar for unconditionally adding a
		 * '''value''' to a [[io.gatling.core.session.Session]].
		 */
		final def add (value : ValueType) : Session => Session =
			_.set (entryName, value)


		/**
		 * The remove method provides syntactic sugar for unconditionally
		 * removing the '''SessionKey''', if any, from a
		 * [[io.gatling.core.session.Session]].
		 */
		final def remove () : Session => Session =
			_ remove entryName


		/// Implicit Conversions
		implicit val definitionInstance : KeyT = this
	}
}
