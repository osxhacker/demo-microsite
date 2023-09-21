package com.github.osxhacker.demo.company.domain.scenario

import cats.{
	Applicative,
	Monad
	}

import org.typelevel.log4cats.SelfAwareStructuredLogger

import com.github.osxhacker.demo.chassis.monitoring.logging.AbstractChangeReport
import com.github.osxhacker.demo.company.domain.{
	Company,
	ScopedEnvironment
	}


/**
 * The '''InferChangeReport''' `object` defines the algorithm for producing a
 * report of significant changes relevant to the
 * [[com.github.osxhacker.demo.company.domain.Company]] entity.  Key to this is
 * what exists in the persistent store ''before'' and ''after'' a persistence
 * operation.  [[cats.data.Ior]] is the ideal representation of this.
 *
 * @see [[com.github.osxhacker.demo.chassis.monitoring.logging.AbstractChangeReport]]
 */
private[scenario] object InferChangeReport
	extends AbstractChangeReport[Company, ScopedEnvironment] ()
{
	/// Class Imports
	import cats.syntax.all._


	/// Instance Properties
	private val slugChanged : (Company, Company) => Option[String] = {
		case (before, after) =>
			Option.unless (before.slug === after.slug) (
				new StringBuilder ()
					.append ("changed slug from '")
					.append (before.slug.show)
					.append ("' to '")
					.append (after.slug.show)
					.append ('\'')
					.result ()
				)
		}

	private val statusChanged : (Company, Company) => Option[String] = {
		case (before, after) =>
			Option.unless (before.status === after.status) (
				new StringBuilder ()
					.append ("changed status from '")
					.append (before.status.show)
					.append ("' to '")
					.append (after.status.show)
					.append ("' slug=")
					.append (after.slug.show)
					.result ()
				)
		}


	override protected def createLogger[F[_]] (env : ScopedEnvironment[F])
		: F[SelfAwareStructuredLogger[F]] =
		env.loggingFactory.create


	override protected def created[F[_]] (after : Company)
		(implicit applicative : Applicative[F])
		: ReportType[F] =
		tell (s"created company slug=${after.slug.show}".some)


	override protected def deleted[F[_]] (before : Company)
		(implicit applicative : Applicative[F])
		: ReportType[F] =
		tell (s"deleted company slug=${before.slug.show}".some)


	override protected def modified[F[_]] (before : Company, after : Company)
		(implicit monad : Monad[F])
		: ReportType[F] =
		tell (slugChanged (before, after)) >>
		tell (statusChanged (before, after))
}

