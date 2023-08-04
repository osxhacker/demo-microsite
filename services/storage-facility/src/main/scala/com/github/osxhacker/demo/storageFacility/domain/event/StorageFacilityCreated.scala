package com.github.osxhacker.demo.storageFacility.domain.event

import cats.ApplicativeThrow
import io.scalaland.chimney
import monocle.macros.Lenses
import squants.space.Volume

import com.github.osxhacker.demo.chassis.domain.entity._
import com.github.osxhacker.demo.chassis.domain.error.LogicError
import com.github.osxhacker.demo.chassis.domain.event.{
	Region,
	ServiceFingerprint
	}

import com.github.osxhacker.demo.chassis.monitoring.CorrelationId
import com.github.osxhacker.demo.storageFacility.domain._


/**
 * The '''StorageFacilityCreated''' type is the Domain Object Model
 * representation of an event which is emitted when a
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]] has been
 * successfully created.  Note that '''StorageFacilityCreated''' is the __only__
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]] event
 * which will have all properties of a
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]].
 */
@Lenses ()
final case class StorageFacilityCreated (
	override val region : Region,
	override val fingerprint : Option[ServiceFingerprint],
	override val correlationId : CorrelationId,
	override val id : Identifier[StorageFacility],
	override val owner : Identifier[Company],
	val status : StorageFacilityStatus,
	val name : StorageFacility.Name,
	val city : StorageFacility.City,
	val state : StorageFacility.State,
	val zip : StorageFacility.Zip,
	val capacity : Volume,
	val available : Volume,
	val timestamps : ModificationTimes
	)
	extends StorageFacilityEvent
{
	/// Class Imports
	import cats.syntax.all._
	import chimney.dsl._
	import mouse.boolean._


	/**
	 * The toStorageFacility method creates a
	 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]]
	 * instance from '''this''' event and the given '''tenant'''.
	 */
	def toStorageFacility[F[_]] (tenant : Company)
		(implicit applicativeThrow : ApplicativeThrow[F])
		: F[StorageFacility] =
		(Company.id.get (tenant) === owner).fold (
			this.into[StorageFacility]
				.withFieldConst (_.owner, tenant)
				.withFieldConst (_.version, Version.initial)
				.withFieldComputed (_.primary, _.region.some)
				.transform
				.pure[F],

			LogicError ("tenant is not the owner of this event").raiseError
			)
}


object StorageFacilityCreated
{
	/// Class Imports
	import chimney.dsl._


	/**
	 * This version of the apply method is provided to allow functional-style
	 * creation from the '''facility''' and available `implicit`
	 * [[com.github.osxhacker.demo.storageFacility.domain.ScopedEnvironment]].
	 */
	def apply[F[_]] (facility : StorageFacility)
		(implicit env : ScopedEnvironment[F])
		: StorageFacilityCreated =
		facility.into[StorageFacilityCreated]
			.withFieldConst (_.correlationId, env.correlationId)
			.withFieldConst (_.fingerprint, None)
			.withFieldConst (_.region, env.region)
			.withFieldComputed (
				_.owner,
				StorageFacility.owner
					.andThen (Company.id)
					.get
				)
			.transform
}

