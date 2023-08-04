package com.github.osxhacker.demo.storageFacility.domain.scenario

import org.typelevel.log4cats
import org.typelevel.log4cats.Logger

import com.github.osxhacker.demo.chassis
import com.github.osxhacker.demo.chassis.domain.event.{
	EmitEvents,
	EventPolicy
	}

import com.github.osxhacker.demo.chassis.domain.repository.UpdateIntent
import com.github.osxhacker.demo.chassis.effect.Pointcut
import com.github.osxhacker.demo.chassis.monitoring.metrics.UseCaseScenario
import com.github.osxhacker.demo.storageFacility.domain._
import com.github.osxhacker.demo.storageFacility.domain.event.AllStorageFacilityEvents
import com.github.osxhacker.demo.storageFacility.domain.specification.FacilityBelongsTo


/**
 * The '''DeleteFacility''' type defines the Use-Case scenario responsible for
 * deleting an existing
 * [[com.github.osxhacker.demo.storageFacility.domain.Company]].  To do so, this
 * scenario __must__ perform a "cascading delete" to ensure there are no
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]]
 * instances __before__ attempting to delete the
 * [[com.github.osxhacker.demo.storageFacility.domain.Company]].
 *
 * Therefore, the workflow for this Use-Case scenario is:
 *
 *   1. Alter the [[com.github.osxhacker.demo.storageFacility.domain.Company]]
 *     status to be
 *     [[com.github.osxhacker.demo.storageFacility.domain.CompanyStatus.Inactive]].
 *
 *   1. Invoke
 *     [[com.github.osxhacker.demo.storageFacility.domain.scenario.DeleteFacility]]
 *     for each
 *     [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]]
 *     owned by the [[com.github.osxhacker.demo.storageFacility.domain.Company]].
 *
 *   1. Finally, delete the target
 *     [[com.github.osxhacker.demo.storageFacility.domain.Company]].
 */
final class DeleteCompany[F[_]] private ()
	(
		implicit

		/// Needed for `compile`.
		private val compiler : fs2.Compiler.Target[F],

		/// Needed for `Aspect`.
		private val pointcut : Pointcut[F],

		/// Needed for `broadcast`.
		private val policy : EventPolicy[
			F,
			ScopedEnvironment[F],
			AllStorageFacilityEvents
			]
	)
{
	/// Class Imports
	import cats.syntax.all._
	import chassis.syntax._
	import log4cats.syntax._
	import mouse.boolean._


	/// Instance Properties
	private val deleteFacility = DeleteFacility[F] ().enableMultiRegion ()
	private val saveCompany = SaveCompany[F] ()


	override def toString () : String = "scenario: cascading delete company"


	def apply (company : Company)
		(implicit env : ScopedEnvironment[F])
		: F[Boolean] =
		delete (company).measure[
			UseCaseScenario[
				F,
				DeleteFacility[F],
				Boolean
				]
			] ()


	private def deactivate (company : Company)
		(implicit env : ScopedEnvironment[F])
		: F[Company] =
		saveCompany[UpdateIntent] (
			Company.status
				.replace (CompanyStatus.Inactive) (company)
			)


	private def delete (company : Company)
		(implicit env : ScopedEnvironment[F])
		: F[Boolean] =
		for {
			implicit0 (logger : Logger[F]) <- env.loggingFactory.create

			_ <- debug"${toString ()} - ${company.id.show}"
			_ <- debug"${toString ()} - making company 'inactive'"
			inactive <- deactivate (company)

			_ <- debug"${toString ()} - deleting all facilities"
			howMany <- deleteFacilitiesOwnedBy (inactive)

			_ <- debug"${toString ()} - deleting company"
			result <- env.companies.delete (inactive)

			_ <- debug"${toString ()} - ${company.id.show} deleted? $result (facilities=$howMany)"
			} yield result


	private def deleteFacilitiesOwnedBy (company : Company)
		(implicit env : ScopedEnvironment[F])
		: F[Int] =
		env.storageFacilities
			.queryBy (FacilityBelongsTo (company))
			.evalMap (deleteFacility (_))
			.map (_.fold (1, 0))
			.compile
			.foldMonoid
}


object DeleteCompany
{
	/// Class Types
	/**
	 * This version of the '''PartiallyApplied''' idiom differs from most in
	 * that its primary purpose is to ensure an "always emit"
	 * [[com.github.osxhacker.demo.chassis.domain.event.EventPolicy]] is
	 * unconditionally used for the '''DeleteCompany''' Use-Case scenario.
	 */
	final class PartiallyApplied[F[_]] ()
		extends EmitEvents[ScopedEnvironment[F], AllStorageFacilityEvents]
	{
		def apply ()
			(
				implicit
				compile : fs2.Compiler.Target[F],
				pointcut : Pointcut[F]
			)
			: DeleteCompany[F] =
			new DeleteCompany[F] ()
	}


	/**
	 * The apply method is provided to support functional-style creation and
	 * employs the "partially applied" idiom, thus only requiring collaborators
	 * to provide ''F'' and allow the compiler to deduce the remaining type
	 * parameters.
	 */
	@inline
	def apply[F[_]] : PartiallyApplied[F] = new PartiallyApplied[F] ()
}
