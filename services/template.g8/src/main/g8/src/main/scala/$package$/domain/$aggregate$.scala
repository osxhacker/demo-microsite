package $package$.domain

import java.util.Objects

import cats.{
	ApplicativeThrow,
	Eq
	}

import com.softwaremill.diffx
import com.softwaremill.diffx.Diff
import eu.timepit.refined
import eu.timepit.refined.api.RefinedTypeOps
import monocle.macros.Lenses

import com.github.osxhacker.demo.chassis.domain.Specification
import com.github.osxhacker.demo.chassis.domain.entity._
import com.github.osxhacker.demo.chassis.domain.error.DomainValueError


/**
 * The '''$aggregate$''' type defines the Domain Object Model representation of
 * $name$.  It is a model of an aggregate root and, as such, equality is
 * determined strictly by its `id` and `version`.
 */
@Lenses ()
final case class $aggregate$ (
	val id : Identifier[$aggregate$],
	val version : Version,
	val status : $aggregate$Status,
	val timestamps : ModificationTimes
	)
{
	/// Class Imports
	import cats.syntax.either._
	import cats.syntax.eq._
	import cats.syntax.functor._
	import diffx.compare
	import mouse.boolean._


	override def equals (that : Any) : Boolean =
		canEqual (that) && {
			val other = that.asInstanceOf[$aggregate$]
			id === other.id && version === other.version
			}


	override def hashCode () : Int = Objects.hash (id, version)


	/**
	 * The changeStatusTo method attempts to update '''this''' `status` to be
	 * the '''candidate''' given, if the current
	 * [[$package$.domain.$aggregate$Status]]
	 * allows it.
	 */
	def changeStatusTo[F[_]] (candidate : $aggregate$Status)
		(implicit applicativeThrow : ApplicativeThrow[F])
		: F[$aggregate$] =
		Either.cond (
			status.canBecome (candidate),
			copy (status = candidate),
			DomainValueError (
				s"cannot change status from '\$status' to '\$candidate'"
				)
			)
			.liftTo[F]


	/**
	 * The differsFrom method determines whether or not '''this''' instance is
	 * different than the '''other''' one given.  It takes into account
	 * properties to ignore and custom comparisons (as needed) as determined by
	 * the `implicit` '''algorithm'''.
	 */
	def differsFrom (other : $aggregate$)
		(implicit algorithm : Diff[$aggregate$])
		: Boolean =
		compare (this, other).isIdentical === false


	/**
	 * The touch method attempts to increment the `version` and ensure that the
	 * `timestamps` are `touch`ed as well.
	 */
	def touch[F[_]] ()
		(implicit applicativeThrow : ApplicativeThrow[F])
		: F[$aggregate$] =
		version.next[F] ()
			.map {
				$aggregate$.version
					.replace (_)
					.andThen (
						$aggregate$.timestamps
							.modify (ModificationTimes.touch)
						)
					.apply (this)
				}


	/**
	 * The unless method is a higher-kinded functor which conditionally invokes
	 * the given functor '''f''' with '''this''' iff `specification` evaluates
	 * '''this''' to be `false`.
	 */
	def unless[A] (specification : Specification[$aggregate$])
		(f : $aggregate$ => A)
		: Option[A] =
		when (!specification) (f)


	/**
	 * The when method is a higher-kinded functor which conditionally invokes
	 * the given functor '''f''' with '''this''' iff the '''specification'''
	 * evaluates '''this''' to be `true`.
	 */
	def when[A] (specification : Specification[$aggregate$])
		(f : $aggregate$ => A)
		: Option[A] =
		specification (this).option (f (this))
}


object $aggregate$
{
	/// Class Imports
	import diffx.generic.auto._
	import diffx.refined._
	import refined.auto._


	/// Implicit Conversions
	implicit val $name;format="camel"$Diff : Diff[$aggregate$] =
		Diff.derived[$aggregate$]
			.ignore (_.timestamps)

	implicit val $name;format="camel"$Eq : Eq[$aggregate$] =
		Eq.and[$aggregate$] (Eq.by (_.id), Eq.by (_.version))

	implicit val $name;format="camel"$Namespace
		: Identifier.EntityNamespace[$aggregate$] =
		Identifier.namespaceFor[$aggregate$] ("$name;format="norm"$")
}

