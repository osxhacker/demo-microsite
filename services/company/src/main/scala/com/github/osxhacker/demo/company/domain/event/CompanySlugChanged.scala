package com.github.osxhacker.demo.company.domain.event

import io.scalaland.chimney
import monocle.macros.Lenses

import com.github.osxhacker.demo.chassis.domain.Slug
import com.github.osxhacker.demo.chassis.domain.entity.Identifier
import com.github.osxhacker.demo.chassis.domain.event.{
	Region,
	ServiceFingerprint
	}

import com.github.osxhacker.demo.chassis.monitoring.CorrelationId
import com.github.osxhacker.demo.company.domain._


/**
 * The '''CompanySlugChanged''' type is the Domain Object Model representation
 * of an event which is emitted when a
 * [[com.github.osxhacker.demo.company.domain.Company]] has its
 * [[com.github.osxhacker.demo.chassis.domain.Slug]] altered.
 */
@Lenses ()
final case class CompanySlugChanged (
	override val region : Region,
	override val fingerprint : Option[ServiceFingerprint],
	override val correlationId : CorrelationId,
	override val id : Identifier[Company],
	val from : Slug,
	val to : Slug
	)
	extends CompanyEvent


object CompanySlugChanged
{
	/// Class Imports
	import chimney.dsl._


	/**
	 * This version of the apply method is provided to allow functional-style
	 * creation from the '''company''' and what the
	 * [[com.github.osxhacker.demo.chassis.domain.Slug]] was '''originally''',
	 * along with the available `implicit`
	 * [[com.github.osxhacker.demo.company.domain.ScopedEnvironment]].
	 */
	def apply[F[_]] (company : Company, originally : Slug)
		(implicit env : ScopedEnvironment[F])
		: CompanySlugChanged =
		company.into[CompanySlugChanged]
			.withFieldConst (_.correlationId, env.correlationId)
			.withFieldConst (_.fingerprint, Option (env.fingerprint))
			.withFieldConst (_.region, env.region)
			.withFieldConst (_.from, originally)
			.withFieldConst (_.to, company.slug)
			.transform
}

