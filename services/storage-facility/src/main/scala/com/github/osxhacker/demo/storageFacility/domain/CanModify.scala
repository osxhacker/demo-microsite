package com.github.osxhacker.demo.storageFacility.domain

import cats.ApplicativeThrow

import com.github.osxhacker.demo.chassis.domain.error.InvalidModelStateError

import specification.{
	CompanyIsActive,
	FacilityIsReadOnly
	}


/**
 * The '''CanModify''' type defines the Domain Object Model concept of
 * dynamically determining whether or not
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]]
 * instances can be modified, based on the chosen policy.
 */
sealed trait CanModify
{
	def apply[F[_]] (facility : StorageFacility)
		(
			implicit
			applicativeThrow : ApplicativeThrow[F],
			env : ScopedEnvironment[F]
		)
		: F[StorageFacility]
}


object CanModify
{
	/// Class Types
	/**
	 * The '''AlwaysAllow''' `object` defines a policy which allows any
	 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]] to
	 * be altered.
	 */
	object AlwaysAllow
		extends CanModify
	{
		/// Class Imports
		import cats.syntax.applicative._


		override def apply[F[_]] (facility : StorageFacility)
			(
				implicit
				applicativeThrow : ApplicativeThrow[F],
				env : ScopedEnvironment[F]
			)
			: F[StorageFacility] =
			facility.pure
	}


	/**
	 * The '''PrimaryRegion''' `object` defines a policy which only allows a
	 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]] when
	 * the following rules are satisfied:
	 *
	 *   - The `primary`
	 *     [[com.github.osxhacker.demo.chassis.domain.event.Region]] is that of
	 *     the given
	 *     [[com.github.osxhacker.demo.storageFacility.domain.ScopedEnvironment]].
	 *
	 *   - The `owner` satisfies
	 *     [[com.github.osxhacker.demo.storageFacility.domain.specification.CompanyIsActive]].
	 */
	object PrimaryRegion
		extends CanModify
	{
		/// Class Imports
		import cats.syntax.either._
		import mouse.boolean._


		/// Instance Properties
		private val companyIsActive = CompanyIsActive (StorageFacility.owner)
		private val facilityIsReadOnly = FacilityIsReadOnly ()


		override def apply[F[_]] (facility : StorageFacility)
			(
				implicit
				applicativeThrow : ApplicativeThrow[F],
				env : ScopedEnvironment[F]
			)
			: F[StorageFacility] =
			verify (facility).toLeft (facility)
				.liftTo[F]


		private def checkCompany (facility : StorageFacility)
			: Option[Throwable] =
			facility.unless (companyIsActive) {
				fac =>
					InvalidModelStateError (
						StorageFacility.id
							.get (fac),

						StorageFacility.version
							.get (fac),

						"company is not active"
						)
				}


		private def isReadOnly[F[_]] (facility : StorageFacility)
			(implicit env : ScopedEnvironment[F])
			: Option[Throwable] =
			facilityIsReadOnly (facility -> env.region).option {
				InvalidModelStateError (
					StorageFacility.id
						.get (facility),

					StorageFacility.version
						.get (facility),

					"storage facility definition resides in a different region"
					)
				}


		private def verify[F[_]] (facility : StorageFacility)
			(implicit env : ScopedEnvironment[F])
			: Option[Throwable] =
			checkCompany (facility) orElse
			isReadOnly (facility)
	}
}

