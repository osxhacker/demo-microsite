package com.github.osxhacker.demo.company.domain.scenario

import scala.language.postfixOps

import cats.Monad
import org.typelevel.log4cats
import org.typelevel.log4cats.Logger

import com.github.osxhacker.demo.chassis
import com.github.osxhacker.demo.chassis.domain.repository.Intent
import com.github.osxhacker.demo.chassis.effect.Pointcut
import com.github.osxhacker.demo.chassis.monitoring.metrics.UseCaseScenario
import com.github.osxhacker.demo.company.domain.{
	ScopedEnvironment,
	Company
	}


/**
 * The '''SaveFacility''' type defines the Use-Case scenario responsible for
 * attempting to persist a [[com.github.osxhacker.demo.company.domain.Company]]
 * based on an [[com.github.osxhacker.demo.chassis.domain.repository.Intent]]
 * given.
 */
final case class SaveCompany[F[_]] ()
	(
		implicit

		/// Needed for `flatMap`.
		private val monad : Monad[F],

		/// Needed for `measure`.
		private val pointcut : Pointcut[F]
	)
{
	/// Class Imports
	import cats.syntax.all._
	import chassis.syntax._
	import log4cats.syntax._


	override def toString () : String = "scenario: save company"


	def apply (intent : Intent[Company])
		(implicit env : ScopedEnvironment[F])
		: F[Option[Company]] =
		save (intent).measure[
			UseCaseScenario[
				F,
				SaveCompany[F],
				Option[Company]
				]
			] ()


	private def save (intent : Intent[Company])
		(implicit env : ScopedEnvironment[F])
		: F[Option[Company]] =
		intent.fold (
			none[Company].pure[F],
			company =>
				for {
					implicit0 (logger : Logger[F]) <- env.loggingFactory.create

					_ <- debug"saving company: ${company.id.show}"
					result <- env.companies.save (intent)
					_ <- debug"saved company: ${result.map (_.id).show}"
					} yield result
			)
}

