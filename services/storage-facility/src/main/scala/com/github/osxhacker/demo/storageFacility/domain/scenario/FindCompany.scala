package com.github.osxhacker.demo.storageFacility.domain.scenario

import com.github.osxhacker.demo.chassis
import com.github.osxhacker.demo.chassis.domain.entity.Identifier
import com.github.osxhacker.demo.chassis.effect.{
	Aspect,
	Pointcut
	}

import com.github.osxhacker.demo.chassis.monitoring.metrics.UseCaseScenario
import com.github.osxhacker.demo.storageFacility.domain.{
	Company,
	CompanyReference,
	ScopedEnvironment
	}


/**
 * The '''FindCompany''' type defines the Use-Case scenario responsible for
 * retrieving a [[com.github.osxhacker.demo.storageFacility.domain.Company]]
 * based only on its
 * [[com.github.osxhacker.demo.chassis.domain.entity.Identifier]] __or__ its
 * [[com.github.osxhacker.demo.storageFacility.domain.CompanyReference]].
 *
 * Where most ''FindEntity'' scenarios use [[monocle.Lens]]es to abstract
 * property use, '''FindCompany''' overloads the `apply` method accepting one
 * of the identification forms directly.
 */
final case class FindCompany[F[_]] ()
	(
		implicit

		/// Needed for `measure`.
		private val pointcut : Pointcut[F]
	)
{
	/// Class Imports
	import chassis.syntax._


	/// Instance Properties
	implicit lazy val cachedAspect = Aspect[
		F,
		UseCaseScenario[F, FindCompany[F], Company]
		].static ()


	override def toString () : String = "scenario: find company"


	/**
	 * This version of the apply method attempts to `find` a
	 * [[com.github.osxhacker.demo.storageFacility.domain.Company]] by its
	 * [[com.github.osxhacker.demo.storageFacility.domain.CompanyReference]].
	 * If it does not exist or there is a problem retrieving it, an error is
	 * raised in ''F''.
	 */
	def apply (reference : CompanyReference)
		(implicit env : ScopedEnvironment[F])
		: F[Company] =
		env.companies
			.find (reference)
			.measure[UseCaseScenario[F, FindCompany[F], Company]] ()


	/**
	 * This version of the apply method attempts to `find` a
	 * [[com.github.osxhacker.demo.storageFacility.domain.Company]] by its
	 * [[com.github.osxhacker.demo.chassis.domain.entity.Identifier]].
	 * If it does not exist or there is a problem retrieving it, an error is
	 * raised in ''F''.
	 */
	def apply (id : Identifier[Company])
		(implicit env : ScopedEnvironment[F])
		: F[Company] =
		env.companies
			.find (id)
			.measure[UseCaseScenario[F, FindCompany[F], Company]] ()
}

