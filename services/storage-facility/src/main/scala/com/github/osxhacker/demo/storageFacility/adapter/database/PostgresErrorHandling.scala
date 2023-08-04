package com.github.osxhacker.demo.storageFacility.adapter.database

import java.sql.SQLException

import scala.collection.immutable.ListMap
import scala.reflect.ClassTag

import cats.{ApplicativeThrow, Semigroup}
import cats.data.Kleisli
import doobie.enumerated.SqlState
import monocle.Getter
import com.github.osxhacker.demo.chassis.domain.entity.{
	Identifier,
	Version
	}

import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.chassis.domain.error._


/**
 * The '''PostgresErrorHandling''' type provides the ability to define what
 * [[org.postgresql]] errors are handled when executing [[doobie]]-defined SQL.
 * The expected use is to define individual error handlers within a repository
 * on a per-operation basis.  For example:
 *
 * {{{
 *     object errorHandlers
 *         extends PostgresErrorHandling[StorageFacility]
 *     {
 *         import cats.syntax.semigroup._
 *
 *
 *         /// For use with attemptSqlState.
 *         val whenDeleting = stale (
 *             StorageFacility.id,
 *             StorageFacility.version
 *             )
 *             .orDefault ("unable to delete storage facility")
 *
 *         /// For use with attemptSomeSqlState.
 *         val whenSaving =
 *             stale (
 *                 StorageFacility.id,
 *                 StorageFacility.version
 *                 ) |+|
 *             duplicate ()
 *     }
 * }}}
 *
 * In each category of [[org.postgresql]] errors handled, the ''A'' type is an
 * arbitrary result which serves as the starting point for providing a derived
 * instance.
 */
abstract class PostgresErrorHandling[DomainT] ()
	(implicit classTag : ClassTag[DomainT])
{
	/// Class Imports
	import PostgresErrorHandling._
	import cats.syntax.option._
	import doobie.postgres._


	/// Instance Properties
	private lazy val domainClassName = classTag.runtimeClass.getSimpleName


	protected def constraintViolation[A] (id : Getter[A, Identifier[DomainT]])
		: PartialHandler[A] =
		handler (
			sqlstate.class23.CHECK_VIOLATION ::
			sqlstate.class23.FOREIGN_KEY_VIOLATION ::
			sqlstate.class23.INTEGRITY_CONSTRAINT_VIOLATION ::
			sqlstate.class23.NOT_NULL_VIOLATION ::
			sqlstate.class23.RESTRICT_VIOLATION ::
			sqlstate.class23.UNIQUE_VIOLATION ::
			Nil
			) {
			(a, ex) =>
				LogicError (
					new StringBuilder ()
						.append (domainClassName)
						.append (" violated persistent storage rule(s): ")
						.append (id.get (a))
						.append (" (")
						.append (ex.getSQLState)
						.append (')')
						.toString (),

					ex.some
				)
		}


	protected def constraintViolation[A] (
		id : Getter[A, Identifier[DomainT]],
		version : Getter[A, Version]
		)
		: PartialHandler[A] =
		handler (
			sqlstate.class23.CHECK_VIOLATION ::
			sqlstate.class23.FOREIGN_KEY_VIOLATION ::
			sqlstate.class23.INTEGRITY_CONSTRAINT_VIOLATION ::
			sqlstate.class23.NOT_NULL_VIOLATION ::
			sqlstate.class23.RESTRICT_VIOLATION ::
			sqlstate.class23.UNIQUE_VIOLATION ::
			Nil
			) {
			(a, ex) =>
				LogicError (
					new StringBuilder ()
						.append (domainClassName)
						.append (" violated persistent storage rule(s): ")
						.append (id.get (a))
						.append (' ')
						.append (version.get (a))
						.append (" (")
						.append (ex.getSQLState)
						.append (')')
						.toString (),

					ex.some
					)
			}


	protected def duplicate[A] () : PartialHandler[A] =
		handler (sqlstate.class23.UNIQUE_VIOLATION) {
			(_, ex) =>
				ConflictingObjectsError[DomainT] (
					s"duplicate $domainClassName instance detected",
					ex.some
					)
			}


	protected def notFound[A] (id : Getter[A, Identifier[DomainT]])
		: PartialHandler[A] =
		handler (sqlstate.class02.NO_DATA) {
			(a, ex) =>
				ObjectNotFoundError[DomainT] (id.get (a), ex.some)
			}


	protected def stale[A] (
		id : Getter[A, Identifier[DomainT]],
		version : Getter[A, Version]
		)
		: PartialHandler[A] =
		handler (sqlstate.class02.NO_DATA) {
			(a, ex) =>
				StaleObjectError[DomainT] (
					id.get (a),
					version.get (a),
					cause = ex.some
				)
			}


	private def handler[A] (sqlState : SqlState)
		(f : (A, SQLException) => RuntimeException)
		: PartialHandler[A] =
		PartialHandler (sqlState) (f)


	private def handler[A] (sqlStates : Seq[SqlState])
		(f : (A, SQLException) => RuntimeException)
		: PartialHandler[A] =
		PartialHandler (ListMap.from (sqlStates.map (_ -> f)))
}


object PostgresErrorHandling
{
	/// Class Types
	/**
	 * The '''PartialHandler''' type defines the ability to handle specific
	 * [[java.sql.SQLException]]s so that an ''A''-specific
	 * ''RuntimeException'' can be produced based on known [[doobie.SqlState]]s.
	 * In this way, it is conceptually similar to [[scala.PartialFunction]] in
	 * that each instance is expected to have only partial value coverage.
	 *
	 * Note that '''PartialHandler''' instances can __only__ be used with
	 * `attemptSomeSql`.
	 *
	 * @see [[com.github.osxhacker.demo.storageFacility.adapter.database.PostgresErrorHandling.Handler]]
	 */
	final case class PartialHandler[A] (
		private val cases : ListMap[
			SqlState,
			(A, SQLException) => RuntimeException
			]
		)
		extends (A => PartialFunction[SQLException, RuntimeException])
	{
		/// Class Imports
		import cats.syntax.option._


		override def apply (a : A)
			: PartialFunction[SQLException, RuntimeException] =
			new PartialFunction[SQLException, RuntimeException] {
				override def apply (ex : SQLException) : RuntimeException =
				{
					val sqlState = SqlState (ex.getSQLState)

					cases (sqlState) (a, ex)
				}


				override def isDefinedAt (ex : SQLException) : Boolean =
					cases.contains (SqlState (ex.getSQLState))
				}


		/**
		 * The orDefault method produces a
		 * [[com.github.osxhacker.demo.storageFacility.adapter.database.PostgresErrorHandling.Handler]]
		 * instance by using the given '''message''' to ensure all
		 * [[java.sql.SQLException]]s __not__ handled by the known `cases`
		 * result in an
		 * [[com.github.osxhacker.demo.chassis.domain.error.UnknownPersistenceError]].
		 */
		def orDefault[R] (message : String) : Handler[A, R] =
			orHandleWith[R] {
				case (_, ex) =>
					UnknownPersistenceError (message, ex.some)
				}


		/**
		 * The orHandleWith method produces a
		 * [[com.github.osxhacker.demo.storageFacility.adapter.database.PostgresErrorHandling.Handler]]
		 * instance which uses the '''default''' functor for
		 * [[java.sql.SQLException]]s not handled by the known `cases`.
		 */
		def orHandleWith[R] (default : (A, SQLException) => RuntimeException)
			: Handler[A, R] =
			Handler (this, default)
	}


	object PartialHandler
	{
		/**
		 * This version of the apply method is provided to support
		 * functional-style '''PartialHandler''' creation when only one case
		 * is provided.
		 */
		def apply[A] (sqlState : SqlState)
			(f : (A, SQLException) => RuntimeException)
			: PartialHandler[A] =
			new PartialHandler[A] (ListMap (sqlState -> f))


		/// Implicit Conversions
		implicit def semigroupPartialHandler[A] : Semigroup[PartialHandler[A]] =
			new Semigroup[PartialHandler[A]] {
				override def combine (
					a : PartialHandler[A], b : PartialHandler[A])
					: PartialHandler[A] =
					PartialHandler (a.cases ++ b.cases)
				}
	}


	/**
	 * The '''Handler''' type defines the ability to handle
	 * [[java.sql.SQLException]]s so that an ''A''-specific
	 * ''RuntimeException'' can be produced.  In this way, it is conceptually
	 * similar to [[scala.Function]] in that each instance is expected to handle
	 * __all__ exceptions.
	 *
	 * Note that '''Handler''' instances can __only__ be used with `attemptSql`.
	 *
	 * @see [[com.github.osxhacker.demo.storageFacility.adapter.database.PostgresErrorHandling.PartialHandler]]
	 */
	final case class Handler[A, R] (
		private val partial : PartialHandler[A],
		private val default : (A, SQLException) => RuntimeException,
		private val verify : Kleisli[ErrorOr, (A, R), R] = Handler.allowAll[A, R] ()
		)
	{
		/// Class Imports
		import cats.syntax.either._


		def apply[F[_]] (a : A)
			(implicit applicativeThrow : ApplicativeThrow[F])
			: Either[SQLException, R] => F[R] =
			_.fold (
				partial (a).applyOrElse (_, default (a, _)).asLeft,
				r => verify (a -> r)
				)
				.liftTo[F]


		/**
		 * The ensure method refines a '''Handler''' to ensure that the given
		 * '''predicate''' is satisfied.  If it evaluates to `false` for the
		 * ''R''esult, then '''f''' will be invoked in `apply` such that an
		 * error is produced.
		 */
		def ensure (predicate : R => Boolean)
			(f : (A, R) => RuntimeException)
			: Handler[A, R] =
			copy (
				verify = Kleisli[ErrorOr, (A, R), R] {
					case (a, r) =>
						Either.cond (predicate (r), r, f (a, r))
					}
			)
	}


	object Handler
	{
		/// Class Imports
		import cats.syntax.either._


		/// Instance Properties
		private def allowAll[A, R] () : Kleisli[ErrorOr, (A, R), R] =
			Kleisli (_._2.asRight)
	}
}
