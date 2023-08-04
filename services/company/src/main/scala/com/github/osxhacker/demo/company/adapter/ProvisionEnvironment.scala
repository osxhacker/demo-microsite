package com.github.osxhacker.demo.company.adapter

import cats.effect.{
	Async,
	Ref
	}

import org.typelevel.log4cats.LoggerFactory

import com.github.osxhacker.demo.chassis.domain.Slug
import com.github.osxhacker.demo.chassis.domain.algorithm.GenerateServiceFingerprint
import com.github.osxhacker.demo.chassis.domain.event.{
	EventProducer,
	Region
	}

import com.github.osxhacker.demo.chassis.effect.{
	Pointcut,
	ReadersWriterResource
	}

import com.github.osxhacker.demo.company.adapter.database.InMemoryCompanyRepository
import com.github.osxhacker.demo.company.adapter.kafka.PublishCompanyEvents
import com.github.osxhacker.demo.company.domain.GlobalEnvironment


/**
 * The '''ProvisionEnvironment''' `object` provides the service with the ability
 * to manufacture
 * [[com.github.osxhacker.demo.company.domain.GlobalEnvironment]]s based
 * on the contents of
 * [[com.github.osxhacker.demo.company.adapter.RuntimeSettings]]
 * instances.
 */
object ProvisionEnvironment
{
	/// Class Imports
	import EventProducer.createChannelFor
	import cats.syntax.flatMap._
	import cats.syntax.functor._


	def apply[F[_], A] (settings : RuntimeSettings)
		(f : ReadersWriterResource[F, GlobalEnvironment[F]] => F[A])
		(
			implicit
			make : Ref.Make[F],
			async : Async[F],
			loggerFactory : LoggerFactory[F],
			pointcut : Pointcut[F]
		)
		: F[A] =
		for {
			region <- Region[F] (settings.region)
			fingerprint <- GenerateServiceFingerprint[F] ()
			companyEvents <- createChannelFor (
				PublishCompanyEvents[F] (settings)
				)

			resource <- ReadersWriterResource.from (
				GlobalEnvironment[F] (
					region,
					fingerprint,
					settings.reservedSlugs
						.map (Slug (_))
						.toNes,

					InMemoryCompanyRepository[F] (),
					companyEvents
					)
				)

			result <- f (resource)
			} yield result
}

