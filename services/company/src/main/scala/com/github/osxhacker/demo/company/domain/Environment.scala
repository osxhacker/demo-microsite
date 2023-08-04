package com.github.osxhacker.demo.company.domain

import scala.language.implicitConversions

import cats.{
	Applicative,
	Functor,
	MonadThrow
	}

import cats.data.NonEmptySet
import org.typelevel.log4cats.LoggerFactory

import com.github.osxhacker.demo.chassis.domain.Slug
import com.github.osxhacker.demo.chassis.domain.event._
import com.github.osxhacker.demo.chassis.monitoring.{
	CorrelationId,
	Subsystem
	}

import com.github.osxhacker.demo.chassis.monitoring.logging.ContextualLoggerFactory
import com.github.osxhacker.demo.company.domain.event.{
	AllCompanyEvents,
	EventChannel
	}

import com.github.osxhacker.demo.company.domain.repository.CompanyRepository


/**
 * The '''GlobalEnvironment''' type defines the Domain Object Model concept of
 * the collaborators universally available to service logic.  In this regard,
 * '''GlobalEnvironment''' can be thought of as a functional version of
 * dependency injection when made available in a [[cats.data.ReaderT]] (which
 * itself actually is a [[cats.data.Kleisli]]).
 */
final case class GlobalEnvironment[F[_]] (
	val region : Region,
	val fingerprint : ServiceFingerprint,
	val reservedSlugs : NonEmptySet[Slug],
	val companies : CompanyRepository[F],
	val companyEvents : EventProducer[F, EventChannel, AllCompanyEvents],
	val shutdownMessage : Option[String] = None
	)
{
	/// Class Imports
	import cats.syntax.applicative._
	import cats.syntax.flatMap._
	import cats.syntax.show._


	/// Instance Properties
	val isOffline = shutdownMessage.isDefined
	val isOnline = !isOffline


	/**
	 * The quiesce method indicates to the '''Environment''' that the service
	 * should stop accepting new work and shut down once those "in flight" have
	 * finished.
	 */
	def quiesce (message : String) : GlobalEnvironment[F] =
		copy (shutdownMessage = Some (message))


	/**
	 * This version of the scopeWith method attempts to create a
	 * [[com.github.osxhacker.demo.company.domain.ScopedEnvironment]]
	 * from '''this''' with the '''correlationId''' and requisite collaborators
	 * in the context of ''F''
	 */
	def scopeWith (correlationId : String)
		(
			implicit
			loggingFactory : LoggerFactory[F],
			monadThrow : MonadThrow[F],
			subsystem : Subsystem
		)
		: F[ScopedEnvironment[F]] =
		CorrelationId[F] (correlationId) >>= scopeWith


	/**
	 * This version of the scopeWith method attempts to create a
	 * [[com.github.osxhacker.demo.company.domain.ScopedEnvironment]]
	 * from '''this''' with the '''correlationId''' and requisite collaborators
	 * in the context of ''F''.
	 */
	def scopeWith (correlationId : CorrelationId)
		(
			implicit
			applicative : Applicative[F],
			loggingFactory : LoggerFactory[F],
			subsystem : Subsystem
		)
		: F[ScopedEnvironment[F]] =
		ScopedEnvironment[F] (this, correlationId).pure[F]
}


/**
 * The '''ScopedEnvironment''' type defines the Domain Object Model concept of
 * a [[com.github.osxhacker.demo.company.domain.GlobalEnvironment]]
 * "scoped", or "localized", to have context needed for evaluating a specific
 * workflow.  Behaviour which is __not__ relevant to attempting to satisfy a
 * __single__ workflow is not made available in a '''ScopedEnvironment'''.
 */
final case class ScopedEnvironment[F[_]] (
	val region : Region,
	val fingerprint : ServiceFingerprint,
	val reservedSlugs : NonEmptySet[Slug],
	val companies : CompanyRepository[F]
	)
	(
		implicit
		val companyEvents : EventProducer[F, EventChannel, AllCompanyEvents],
		val correlationId : CorrelationId,
		val loggingFactory : ContextualLoggerFactory[F]
	)
{
	/**
	 * The addContext method creates a new '''ScopedEnvironment''' having a
	 * `loggingFactory` incorporating the given '''properties'''.
	 */
	def addContext (properties : Map[String, String]) : ScopedEnvironment[F] =
		copy () (
			companyEvents,
			correlationId,
			loggingFactory.addContext (properties)
			)
}


object ScopedEnvironment
{
	/// Class Imports
	import cats.syntax.show._


	/**
	 * This version of the apply method creates a '''ScopedEnvironment''' from
	 * the given [[com.github.osxhacker.demo.company.domain.GlobalEnvironment]]
	 * and [[com.github.osxhacker.demo.chassis.monitoring.CorrelationId]]
	 * collaborators.
	 */
	def apply[F[_]] (
		global : GlobalEnvironment[F],
		correlationId : CorrelationId
		)
		(
			implicit
			functor : Functor[F],
			loggingFactory : LoggerFactory[F],
			subsystem : Subsystem
		)
		: ScopedEnvironment[F] =
		ScopedEnvironment[F] (
			global.region,
			global.fingerprint,
			global.reservedSlugs,
			global.companies
			) (
			global.companyEvents,
			correlationId,
			ContextualLoggerFactory[F] (loggingFactory) (
				Map (
					"correlationId" -> correlationId.show,
					"region" -> global.region.show,
					"subsystem" -> subsystem.show
					)
				)
			)


	/// Implicit Conversions
	implicit def companyEventsFromEnv[F[_]]
		: ScopedEnvironment[F] => EventProducer[F, EventChannel, AllCompanyEvents] =
		_.companyEvents
}

