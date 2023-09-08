package com.github.osxhacker.demo.chassis.monitoring.logging

import cats.{
	Applicative,
	Monad
	}

import cats.data.{
	Ior,
	WriterT
	}

import org.typelevel.log4cats.SelfAwareStructuredLogger


/**
 * The '''AbstractChangeReport''' `object` defines the algorithm for
 * producing a report of significant changes relevant to the ''DomainT''
 * entity.  Key to this is what exists in the persistent store ''before'' and
 * ''after'' a persistence operation.  [[cats.data.Ior]] is the ideal
 * representation of this.  ''HavingCreated'', ''HavingDeleted'', and
 * ''HavingModified'' are defined to enhance semantic value beyond the contract
 * established by [[cats.data.Ior]].
 *
 * When there was no ''DomainT'' ''before'' and now there is one ''after'', the
 * ''DomainT'' has been created.  This equates to a [[cats.data.Ior.Right]].
 *
 * When there was a ''DomainT'' ''before'' and now there is __not__ one
 * ''after'', the ''DomainT'' has been deleted.  This equates to a
 * [[cats.data.Ior.Left]].
 *
 * With both a ''DomainT'' ''before'' __and__ ''after'', a modification to an
 * existing ''DomainT'' has been performed.  This equates to a
 * [[cats.data.Ior.Both]].
 */
abstract class AbstractChangeReport[DomainT, EnvT[F[_]]] ()
{
	/// Class Imports
	import cats.syntax.all._


	/// Class Types
	final protected type ReportType[F[_]] = WriterT[F, List[String], Unit]


	/// Instance Properties
	/**
	 * The HavingCreated functor exists to provide semantic meaning above what
	 * [[cats.data.Ior.Right]] intrinsically represents.
	 *
	 * @see [[com.github.osxhacker.demo.chassis.monitoring.logging.AbstractChangeReport]]
	 */
	val HavingCreated : DomainT => Ior.Right[DomainT] = Ior.Right[DomainT]

	/**
	 * The HavingDeleted functor exists to provide semantic meaning above what
	 * [[cats.data.Ior.Left]] intrinsically represents.
	 *
	 * @see [[com.github.osxhacker.demo.chassis.monitoring.logging.AbstractChangeReport]]
	 */
	val HavingDeleted : DomainT => Ior.Left[DomainT] = Ior.Left[DomainT]

	/**
	 * The HavingModified functor exists to provide semantic meaning above what
	 * [[cats.data.Ior.Both]] intrinsically represents.
	 *
	 * @see [[com.github.osxhacker.demo.chassis.monitoring.logging.AbstractChangeReport]]
	 */
	val HavingModified : (DomainT, DomainT) => Ior.Both[DomainT, DomainT] =
		Ior.Both (_, _)


	/// Abstract Methods
	protected def createLogger[F[_]] (env : EnvT[F])
		: F[SelfAwareStructuredLogger[F]]


	protected def created[F[_]] (after : DomainT)
		(implicit applicative: Applicative[F])
		: ReportType[F]


	protected def deleted[F[_]] (before : DomainT)
		(implicit applicative : Applicative[F])
		: ReportType[F]


	protected def modified[F[_]] (before : DomainT, after : DomainT)
		(implicit monad : Monad[F])
		: ReportType[F]


	/**
	 * This version of the apply method only evaluates the given '''change'''
	 * if present.
	 */
	final def apply[F[_]] (change : Option[Ior[DomainT, DomainT]])
		(
			implicit
			env : EnvT[F],
			monad : Monad[F]
		)
		: F[Unit] =
		change.fold (monad.unit) (apply[F])


	/**
	 * This version of the apply method unconditionally evaluates the given
	 * '''change'''.  What report is generated is based on the underlying
	 * '''change''':
	 *
	 *   - [[cats.data.Ior.Left]] - a deletion.
	 *   - [[cats.data.Ior.Right]] - a creation.
	 *   - [[cats.data.Ior.Both]] - a modification.
	 */
	final def apply[F[_]] (change : Ior[DomainT, DomainT])
		(
			implicit
			env : EnvT[F],
			monad : Monad[F]
		)
		: F[Unit] =
		change.fold[ReportType[F]] (deleted, created, modified)
			.written (monad)
			.product (createLogger[F] (env))
			.flatMap {
				case (report, log) =>
					report.foldMapM (log.info (_))
				}


	@inline
	protected def tell[F[_]] (message : Option[String])
		(implicit applicative : Applicative[F])
		: ReportType[F] =
		WriterT.tell[F, List[String]] (message.toList)
}

