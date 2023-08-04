package com.github.osxhacker.demo.company.domain.scenario

import cats.ApplicativeThrow
import cats.data.Kleisli
import fs2.Stream

import com.github.osxhacker.demo.chassis
import com.github.osxhacker.demo.chassis.effect.Pointcut
import com.github.osxhacker.demo.chassis.monitoring.metrics.UseCaseScenario
import com.github.osxhacker.demo.company.domain.{
	Company,
	ScopedEnvironment
	}


/**
 * The '''LoadAllCompany''' type defines the Use-Case scenario responsible
 * for retrieving all [[com.github.osxhacker.demo.company.domain.Company]]
 * instances in the persistent store.
 *
 * Note that `factory` is defined in terms of the ''F'' container to allow for
 * arbitrary logic to produce a ''ResultT''.
 */
final case class LoadAllCompanies[F[_]] ()
	(
		implicit

		/// Needed for `pure`.
		private val applicativeThrow : ApplicativeThrow[F],

		/// Needed for `Aspect`.
		private val pointcut : Pointcut[F]
	)
{
	/// Class Imports
	import cats.syntax.all._
	import chassis.syntax._


	override def toString () : String = "scenario: load all companies"


	/**
	 * This version of the apply method supports retrieving all known
	 * [[com.github.osxhacker.demo.company.domain.Company]] instances without
	 * any transformation.
	 */
	def apply ()
		(implicit env : ScopedEnvironment[F])
		: F[fs2.Stream[F, Company]] =
		env.companies
			.findAll ()
			.pure[F]
			.measure[
				UseCaseScenario[
					F,
					LoadAllCompanies[F],
					Stream[F, Company]
					]
				] ()


	/**
	 * This version of the apply method supports retrieving all known
	 * [[com.github.osxhacker.demo.company.domain.Company]] instances with
	 * __each__ transformed by the given '''factory'''.
	 */
	def apply[ResultT] (factory : Kleisli[F, Company, ResultT])
		(implicit env : ScopedEnvironment[F])
	: F[Stream[F, ResultT]] =
		env.companies
			.findAll ()
			.evalMapChunk (factory.run)
			.pure[F]
			.measure[
				UseCaseScenario[
					F,
					LoadAllCompanies[F],
					Stream[F, ResultT]
					]
				] ()
}

