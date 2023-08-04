package com.github.osxhacker.demo.storageFacility.domain.scenario

import cats.MonadThrow
import com.github.osxhacker.demo.chassis
import com.github.osxhacker.demo.chassis.effect.{
	Aspect,
	Pointcut
	}

import com.github.osxhacker.demo.chassis.domain.error.InvalidModelStateError
import com.github.osxhacker.demo.chassis.monitoring.metrics.UseCaseScenario
import com.github.osxhacker.demo.storageFacility.domain.{
	Company,
	CompanyReference,
	ScopedEnvironment
	}

import com.github.osxhacker.demo.storageFacility.domain.specification.CompanyIsActive


/**
 * The '''FindActiveCompany''' type defines the Use-Case scenario specialization
 * of [[com.github.osxhacker.demo.storageFacility.domain.scenario.FindCompany]]
 * which ensures a successfully retrieved
 * [[com.github.osxhacker.demo.storageFacility.domain.Company]] satisfies the
 * [[com.github.osxhacker.demo.storageFacility.domain.specification.CompanyIsActive]]
 * [[com.github.osxhacker.demo.chassis.domain.Specification]], resulting in an
 * error raised in ''F'' if not.
 */
final case class FindActiveCompany[F[_]] ()
	(
		implicit

		/// Needed for `ensureOr`.
		private val monadThrow : MonadThrow[F],

		/// Needed for `measure`.
		private val pointcut : Pointcut[F]
	)
{
	/// Class Imports
	import Company.{
		id,
		status
		}

	import cats.syntax.monadError._
	import cats.syntax.show._
	import chassis.syntax._


	/// Instance Properties
	implicit lazy val cachedAspect = Aspect[
		F,
		UseCaseScenario[F, FindActiveCompany[F], Company]
		].static ()

	private val find = FindCompany[F] ()
	private val isActive = CompanyIsActive ()


	override def toString () : String = "scenario: find active company"


	def apply (reference : CompanyReference)
		(implicit env : ScopedEnvironment[F])
	  	: F[Company] =
		findActive (reference).measure[
			UseCaseScenario[F, FindActiveCompany[F], Company]
			] ()


	private def findActive (reference : CompanyReference)
		(implicit env : ScopedEnvironment[F])
		: F[Company] =
		find (reference).ensureOr (reject) (isActive)


	private def reject (company : Company) : InvalidModelStateError[Company] =
	{
		val currentStatus = status.get (company).show
		val theId = id.get (company).show

		InvalidModelStateError[Company] (
			id.get (company),
			s"${toString ()} - company is not active: $theId ($currentStatus)"
			)
	}
}

