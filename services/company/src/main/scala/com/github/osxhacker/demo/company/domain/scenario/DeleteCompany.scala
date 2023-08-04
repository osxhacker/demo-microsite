package com.github.osxhacker.demo.company.domain.scenario

import cats.MonadThrow
import org.typelevel.log4cats
import org.typelevel.log4cats.Logger

import com.github.osxhacker.demo.chassis
import com.github.osxhacker.demo.chassis.domain.{
	Slug,
	Specification
	}

import com.github.osxhacker.demo.chassis.domain.error.ValidationError
import com.github.osxhacker.demo.chassis.domain.event.EventPolicy
import com.github.osxhacker.demo.chassis.effect.Pointcut
import com.github.osxhacker.demo.chassis.monitoring.metrics.UseCaseScenario
import com.github.osxhacker.demo.company.domain.{
	Company,
	ScopedEnvironment
	}

import com.github.osxhacker.demo.company.domain.event.{
	AllCompanyEvents,
	CompanyDeleted
	}


/**
 * The '''DeleteCompany''' type defines the Use-Case scenario responsible for
 * deleting an existing [[com.github.osxhacker.demo.company.domain.Company]].
 * If the candidate [[com.github.osxhacker.demo.company.domain.Company]] has a
 * "reserved [[com.github.osxhacker.demo.chassis.domain.Slug]]", the deletion
 * fails.
 */
final case class DeleteCompany[F[_]] ()
	(
		implicit

		/// Needed for `flatMap` and `liftTo`.
		private val monadThrow : MonadThrow[F],

		/// Needed for `measure`.
		private val pointcut : Pointcut[F],

		/// Needed for `broadcast`.
		private val policy : EventPolicy[F, ScopedEnvironment[F], AllCompanyEvents]
	)
{
	/// Class Imports
	import cats.syntax.all._
	import chassis.syntax._
	import log4cats.syntax._


	/// Instance Properties
	private val wasDeleted = Specification[Boolean] (_ === true)


	override def toString () : String = "scenario: delete facility"


	def apply (company : Company)
		(implicit env : ScopedEnvironment[F])
		: F[Boolean] =
		delete (company).addEventWhen (wasDeleted) (CompanyDeleted (company))
			.broadcast ()
			.measure[UseCaseScenario[F, DeleteCompany[F], Boolean]] ()


	private def delete (company : Company)
		(implicit env : ScopedEnvironment[F])
		: F[Boolean] =
		for {
			implicit0 (logger : Logger[F]) <- env.loggingFactory.create

			_ <- debug"${toString ()} : ${company.id.show}"
			_ <- failWhenReserved (company)
			result <- env.companies.delete (company)

			_ <- debug"${toString ()} : ${company.id.show} deleted? $result"
			} yield result


	private def failWhenReserved (company : Company)
		(implicit env : ScopedEnvironment[F])
		: F[Slug] =
		ValidateSlug (company, AdaptOptics (Company.slug))
			.leftMap (ValidationError[Company] (_))
			.liftTo[F]
}

