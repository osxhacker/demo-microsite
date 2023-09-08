package com.github.osxhacker.demo.company.domain.scenario

import scala.reflect.ClassTag

import cats.MonadThrow
import eu.timepit.refined.api.Refined
import monocle.{
	Getter,
	Iso
	}

import com.github.osxhacker.demo.chassis
import com.github.osxhacker.demo.chassis.domain.entity.Version
import com.github.osxhacker.demo.chassis.domain.error.{
	StaleObjectError,
	ValidationError
	}

import com.github.osxhacker.demo.chassis.domain.event.{
	EventLog,
	EventPolicy
	}

import com.github.osxhacker.demo.chassis.domain.repository.UpdateIntent
import com.github.osxhacker.demo.chassis.effect.Pointcut
import com.github.osxhacker.demo.chassis.monitoring.metrics.UseCaseScenario
import com.github.osxhacker.demo.company.domain.{
	Company,
	CompanyStatus,
	ScopedEnvironment
	}

import com.github.osxhacker.demo.company.domain.event.{
	AllCompanyEvents,
	CompanyStatusChanged
	}


/**
 * The '''ChangeCompanyStatus''' type defines the Domain Object Model Use-Case
 * scenario responsible for altering a
 * [[com.github.osxhacker.demo.company.domain.Company]] status (if allowed) and
 * then invoking
 * [[com.github.osxhacker.demo.company.domain.scenario.SaveCompany]] to
 * persist it.
 */
final class ChangeCompanyStatus[F[_], SourceT] (
	private val version : AdaptOptics.KleisliType[SourceT, Version]
	)
	(
		implicit

		/// Needed for ''ValidationError''.
		private val classTag : ClassTag[SourceT],

		/// Needed for `flatMap` and `save`.
		private val monadThrow : MonadThrow[F],

		/// Needed for `measure`.
		private val pointcut : Pointcut[F],

		/// Needed for `broadcast`.
		private val policy : EventPolicy[
			F,
			ScopedEnvironment[F],
			AllCompanyEvents
			],
	)
{
	/// Class Imports
	import InferChangeReport.HavingModified
	import cats.syntax.all._
	import chassis.syntax._
	import mouse.foption._


	/// Instance Properties
	private val save = SaveCompany[F] ()


	override def toString () : String = "scenario: change company status"


	def apply (
		existing : Company,
		versionSource : SourceT,
		status : CompanyStatus
		)
		(implicit env : ScopedEnvironment[F])
		: F[Company] =
		changeStatus (existing, versionSource, status).broadcast ()
			.measure[
				UseCaseScenario[
					F,
					ChangeCompanyStatus[F, SourceT],
					Company
					]
				] ()


	private def changeStatus (
		existing : Company,
		versionSource : SourceT,
		desired : CompanyStatus
		)
		(implicit env : ScopedEnvironment[F])
		: EventLog[F, Company, AllCompanyEvents] =
		version.mapF {
			_.leftMap (ValidationError[SourceT] (_))
				.toEither
				.liftTo[F]
			}
			.second[Company]
			.mapF[EventLog[F, *, AllCompanyEvents], Company] {
				_.flatMap ((updateInstanceAndSave (desired) _).tupled)
					.flatTap {
						saved =>
							InferChangeReport (HavingModified (existing, saved))
						}
					.deriveEvent (CompanyStatusChanged (_))
				}
		.run (existing -> versionSource)


	private def updateInstanceAndSave (desired : CompanyStatus)
		(existing : Company, version : Version)
		(implicit env : ScopedEnvironment[F])
		: F[Company] =
		existing.changeStatusTo[F] (desired)
			.map (UpdateIntent (_).filter (_.version === version))
			.flatMap {
				save (_).getOrRaise (
					StaleObjectError[Company] (
						existing.id,
						existing.version,
						existing.some
						)
					)
				}
}


object ChangeCompanyStatus
{
	/// Class Types
	final class PartiallyApplied[F[_]] ()
	{
		/**
		 * This version of the apply method constructs a
		 * '''ChangeCompanyStatus''' scenario which has as its ''SourceT'' the
		 * [[com.github.osxhacker.demo.chassis.domain.entity.Version]] required.
		 */
		def apply ()
			(
				implicit
				monadThrow : MonadThrow[F],
				pointcut : Pointcut[F],
				policy : EventPolicy[F, ScopedEnvironment[F], AllCompanyEvents]
			)
			: ChangeCompanyStatus[F, Version] =
			new ChangeCompanyStatus[F, Version] (AdaptOptics (Iso.id))


		/**
		 * This version of the apply method constructs a scenario which attempts
		 * to use a [[com.github.osxhacker.demo.chassis.domain.entity.Version]]
		 * within an arbitrary ''SourceT''.
		 */
		def apply[SourceT, P1] (
			version : Getter[SourceT, Refined[Int, P1]]
			)
			(
				implicit
				classTag : ClassTag[SourceT],
				monadThrow : MonadThrow[F],
				pointcut : Pointcut[F],
				policy : EventPolicy[F, ScopedEnvironment[F], AllCompanyEvents]
			)
			: ChangeCompanyStatus[F, SourceT] =
			new ChangeCompanyStatus[F, SourceT] (
				version = AdaptOptics.version (version)
				)
	}


	/**
	 * The apply method is provided to support functional-style creation and
	 * employs the "partially applied" idiom, thus only requiring collaborators
	 * to provide ''F'' and allowing the compiler to deduce the remaining type
	 * parameters.
	 */
	@inline
	def apply[F[_]] : PartiallyApplied[F] = new PartiallyApplied[F] ()
}
