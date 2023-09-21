package com.github.osxhacker.demo.storageFacility.domain.scenario

import cats.{
	Applicative,
	Monad
	}

import org.typelevel.log4cats.SelfAwareStructuredLogger

import com.github.osxhacker.demo.chassis.monitoring.logging.AbstractChangeReport
import com.github.osxhacker.demo.storageFacility.domain.{
	StorageFacility,
	ScopedEnvironment
	}


/**
 * The '''InferFacilityChangeReport''' `object` defines the algorithm for
 * producing a report of significant changes relevant to the
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]] entity.
 * Key to this is what exists in the persistent store ''before'' and ''after'' a
 * persistence operation.  [[cats.data.Ior]] is the ideal representation of
 * this.
 *
 * @see [[com.github.osxhacker.demo.chassis.monitoring.logging.AbstractChangeReport]]
 */
private[scenario] object InferFacilityChangeReport
	extends AbstractChangeReport[StorageFacility, ScopedEnvironment] ()
{
	/// Class Imports
	import cats.syntax.all._


	/// Instance Properties
	private val statusChanged
		: (StorageFacility, StorageFacility) => Option[String] = {
		case (before, after) =>
			Option.unless (before.status === after.status) (
				new StringBuilder ()
					.append ("changed facility status from '")
					.append (before.status.show)
					.append ("' to '")
					.append (after.status.show)
					.append ("' id=")
					.append (before.id.show)
					.result ()
				)
		}


	override protected def createLogger[F[_]] (env : ScopedEnvironment[F])
		: F[SelfAwareStructuredLogger[F]] =
		env.loggingFactory
			.create


	override protected def created[F[_]] (after : StorageFacility)
		(implicit applicative : Applicative[F])
		: ReportType[F] =
		tell (s"created facility id=${after.id.show}".some)


	override protected def deleted[F[_]] (before : StorageFacility)
		(implicit applicative : Applicative[F])
		: ReportType[F] =
		tell (s"deleted facility id=${before.id.show}".some)


	override protected def modified[F[_]] (before: StorageFacility, after: StorageFacility)
		(implicit monad: Monad[F])
		: ReportType[F] =
		tell (statusChanged (before, after))
}

