package com.github.osxhacker.demo.company.domain.event

import io.scalaland.chimney
import monocle.macros.Lenses

import com.github.osxhacker.demo.company.domain._
import com.github.osxhacker.demo.chassis.domain.entity.Identifier
import com.github.osxhacker.demo.chassis.domain.event.{
	Region,
	ServiceFingerprint
	}

import com.github.osxhacker.demo.chassis.monitoring.CorrelationId


/**
 * The '''CompanyDeleted''' type is the Domain Object Model representation of an
 * event which is emitted when a
 * [[com.github.osxhacker.demo.company.domain.Company]] has been successfully
 * removed from the persistent store.
 */
@Lenses ()
final case class CompanyDeleted (
	override val region : Region,
	override val fingerprint : Option[ServiceFingerprint],
	override val correlationId : CorrelationId,
	override val id : Identifier[Company]
	)
	extends CompanyEvent


object CompanyDeleted
{
	/// Class Imports
	import chimney.dsl._


	/**
	 * This version of the apply method is provided to allow functional-style
	 * creation from the '''company''' and available `implicit`
	 * [[com.github.osxhacker.demo.company.domain.ScopedEnvironment]].
	 */
	def apply[F[_]] (company : Company)
		(
			implicit
			env : ScopedEnvironment[F]
		)
		: CompanyDeleted =
		company.into[CompanyDeleted]
			.withFieldConst (_.correlationId, env.correlationId)
			.withFieldConst (_.fingerprint, Option (env.fingerprint))
			.withFieldConst (_.region, env.region)
			.transform
}

