package com.github.osxhacker.demo.storageFacility.domain.scenario

import monocle.Getter
import org.typelevel.log4cats.{
	LoggerFactory,
	StructuredLogger
	}

import shapeless.{
	syntax => _,
	_
	}

import com.github.osxhacker.demo.chassis.domain.entity.Identifier
import com.github.osxhacker.demo.chassis.domain.event.{
	Region,
	SuppressEvents
	}

import com.github.osxhacker.demo.chassis.effect.Pointcut
import com.github.osxhacker.demo.chassis.monitoring.{
	CorrelationId,
	Subsystem
	}

import com.github.osxhacker.demo.storageFacility.domain._


/**
 * The '''AbstractEventInterpreter''' type defines common behaviour available
 * for Domain Object Model event interpreters.  This includes the ability to
 * "set up" a
 * [[com.github.osxhacker.demo.storageFacility.domain.ScopedEnvironment]] as
 * well as define contracts/logic for dispatching events.
 */
abstract class AbstractEventInterpreter[
	F[_],
	EntityT <: AnyRef,
	DomainEventsT <: Coproduct
	] ()
	(
		implicit

		/// Needed for ''scenarios''.
		protected val compiler : fs2.Compiler.Target[F],

		/// Needed for logging.
		protected val loggerFactory : LoggerFactory[F],

		/// Needed for ''scenarios''.
		protected val pointcut : Pointcut[F],

		/// Needed for `scopeWith`.
		protected val subsystem : Subsystem
	)
	extends SuppressEvents[ScopedEnvironment[F], DomainEventsT]
{
	/// Class Imports
	import cats.syntax.all._


	/// Class Types
	protected abstract class EventDispatcher
		extends Poly1
	{
		@inline
		protected def handler[EventT] (description: EventT => String)
			(block: EventT => F[Unit])
			(implicit logger : StructuredLogger[F])
			: Case.Aux[EventT, F[Unit]] =
			at {
				event =>
					block (event).recoverWith {
						logger.warn (_) (s"unable to ${description (event)}")
						}
				}
	}


	protected class MakeScopedEnvironment
		extends Poly1
	{
		/// Implicit Conversions
		@inline
		protected def definition[BaseEventT <: AnyRef, EventT <: BaseEventT] (
			id : Getter[BaseEventT, Identifier[EntityT]],
			correlationId : Getter[BaseEventT, CorrelationId],
			companyId : Getter[BaseEventT, Identifier[Company]],
			region : Getter[BaseEventT, Region]
			)
			(implicit global : GlobalEnvironment[F])
			: Case.Aux[EventT, F[ScopedEnvironment[F]]] =
			at {
				event =>
					global.scopeWith (
						CompanyReference (companyId.get (event)),
						correlationId.get (event)
						)
						.map {
							_.addContext (
								Map (
									"eventType" -> event.getClass.getName,
									"id" -> id.get (event).show,
									"region" -> region.get (event).show
									)
								)
							}
				}
	}
}

