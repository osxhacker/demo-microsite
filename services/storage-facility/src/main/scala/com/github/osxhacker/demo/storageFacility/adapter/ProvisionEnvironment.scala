package com.github.osxhacker.demo.storageFacility.adapter

import cats.effect.{
	Async,
	Ref,
	Resource
	}

import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.typelevel.log4cats.LoggerFactory

import com.github.osxhacker.demo.chassis.domain.Slug
import com.github.osxhacker.demo.chassis.domain.event.{
	EventProducer,
	Region
	}

import com.github.osxhacker.demo.chassis.effect.{
	Pointcut,
	ReadersWriterResource
	}

import com.github.osxhacker.demo.storageFacility.adapter.database.{
	DoobieCompany,
	DoobieStorageFacility
	}

import com.github.osxhacker.demo.storageFacility.adapter.kafka.PublishStorageFacilityEvents
import com.github.osxhacker.demo.storageFacility.domain.{
	CompanyReference,
	GlobalEnvironment
	}


/**
 * The '''ProvisionEnvironment''' `object` provides the service with the ability
 * to manufacture
 * [[com.github.osxhacker.demo.storageFacility.domain.GlobalEnvironment]]s based
 * on the contents of
 * [[com.github.osxhacker.demo.storageFacility.adapter.RuntimeSettings]]
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
	{
		val db = mkTransactor[F] (settings)

		db.use {
			xa =>
				for {
					region <- Region[F] (settings.region)
					storageFacilityEvents <- createChannelFor (
						PublishStorageFacilityEvents[F] (settings)
						)

					companies = new DoobieCompany[F] (xa)
					facilities = new DoobieStorageFacility[F] (xa)
					global = GlobalEnvironment[F] (
						region,
						CompanyReference (Slug (settings.operationsSlug)),
						companies,
						facilities,
						storageFacilityEvents
						)

					_ <- companies.createSchema ()
					_ <- facilities.createSchema ()
					managed <- ReadersWriterResource.from (global)
					result <- f (managed)
				} yield result
			}
	}


	private def processors () : Int = Runtime.getRuntime.availableProcessors ()


	private def mkTransactor[F[_]] (settings : RuntimeSettings)
		(implicit async : Async[F])
		: Resource[F, HikariTransactor[F]] =
		for {
			ce <- ExecutionContexts.fixedThreadPool[F] (2 * processors ())
			xa <- HikariTransactor.newHikariTransactor[F] (
				settings.database.driver.value,
				settings.database.url.value,
				settings.database.user.value,
				settings.database.password.value,
				ce
				)
			} yield xa
}

