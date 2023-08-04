package $package$.domain

import cats.{
	Applicative,
	Functor,
	MonadThrow
	}

import org.typelevel.log4cats.LoggerFactory

import com.github.osxhacker.demo.chassis.monitoring.{
	CorrelationId,
	Subsystem
	}

import com.github.osxhacker.demo.chassis.monitoring.logging.ContextualLoggerFactory


/**
 * The '''GlobalEnvironment''' type defines the Domain Object Model concept of
 * the collaborators universally available to service logic.  In this regard,
 * '''GlobalEnvironment''' can be thought of as a functional version of
 * dependency injection when made available in a [[cats.data.ReaderT]] (which
 * itself actually is a [[cats.data.Kleisli]]).
 */
final case class GlobalEnvironment[F[_]] (
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
	 * [[$package$.domain.ScopedEnvironment]]
	 * from '''this''' with the '''correlationId''' and requisite collaborators.
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
	 * [[$package$.domain.ScopedEnvironment]]
	 * from '''this''' with the '''correlationId''' and requisite collaborators.
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
 * a [[$package$.domain.GlobalEnvironment]]
 * "scoped", or "localized", to have context needed for evaluating a specific
 * workflow.  Behaviour which is __not__ relevant to attempting to satisfy a
 * __single__ workflow is not made available in a '''ScopedEnvironment'''.
 */
final case class ScopedEnvironment[F[_]] ()
	(
		implicit
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
	 * the given [[$package$.domain.GlobalEnvironment]]
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
		ScopedEnvironment[F] () (
			correlationId,
			ContextualLoggerFactory[F] (loggingFactory) (
				Map (
					"correlationId" -> correlationId.show,
					"subsystem" -> subsystem.show
					)
				)
			)
}

