package com.github.osxhacker.demo.storageFacility.domain.scenario

import scala.language.postfixOps

import cats.{
	Applicative,
	MonadThrow
	}

import org.typelevel.log4cats
import org.typelevel.log4cats.Logger

import com.github.osxhacker.demo.chassis
import com.github.osxhacker.demo.chassis.domain.error.LogicError
import com.github.osxhacker.demo.chassis.domain.repository.Intent
import com.github.osxhacker.demo.chassis.effect.Pointcut
import com.github.osxhacker.demo.chassis.monitoring.metrics.UseCaseScenario
import com.github.osxhacker.demo.storageFacility.domain.{
	Company,
	ScopedEnvironment
	}


/**
 * The '''SaveCompany''' type defines the Use-Case scenario responsible for
 * attempting to persist a
 * [[com.github.osxhacker.demo.storageFacility.domain.Company]]
 * based on the desired
 * [[com.github.osxhacker.demo.chassis.domain.repository.Intent]] specified in
 * the `apply` invocation.
 *
 * Since the [[com.github.osxhacker.demo.storageFacility.domain.Company]] type
 * is a member of the "reference model" and not an
 * [[https://en.wikipedia.org/wiki/Domain-driven_design#aggregate_root Aggregate root]]
 * managed by the storage-facility microservice, mutations have minimal
 * validations (provided by the Domain Object Model).  This results in
 * '''SaveCompany''' being able to satisfy the functional requirements without
 * need of higher-level Use-Cases.
 */
final case class SaveCompany[F[_]] ()
	(
		implicit

		/// Needed for `getOrRaise`.
		private val monadThrow : MonadThrow[F],

		/// Needed for `measure`.
		private val pointcut : Pointcut[F]
	)
{
	/// Class Imports
	import cats.syntax.all._
	import chassis.syntax._
	import log4cats.syntax._
	import mouse.foption._


	override def toString () : String = "scenario: save company"


	def apply[IntentT[A] <: Intent[A]] (company : Company)
		(
			implicit

			applicative : Applicative[IntentT],
			env : ScopedEnvironment[F],
		)
		: F[Company] =
		save (company.pure[IntentT]).getOrRaise (
			LogicError (s"${toString ()} - did not produce a company")
			)
			.measure[UseCaseScenario[F, SaveCompany[F], Company]] ()


	private def save[IntentT[A] <: Intent[A]] (intent : IntentT[Company])
		(implicit env : ScopedEnvironment[F])
		: F[Option[Company]] =
		intent.fold (
			none[Company].pure[F],
			company =>
				for {
					implicit0 (logger : Logger[F]) <- env.loggingFactory.create

					_ <- debug"${toString ()} - ${company.id.show}"
					result <- env.companies.save (intent)

					_ <- debug"${toString ()} result - ${result.map (_.id).show}"
					} yield result
			)
}

