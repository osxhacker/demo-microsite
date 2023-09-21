package com.github.osxhacker.demo.chassis.monitoring.logging

import scala.collection.mutable.ListBuffer

import cats.effect.Sync
import org.slf4j.{
	LoggerFactory => Slf4jLoggerFactory
	}

import org.slf4j.event.{
	DefaultLoggingEvent,
	Level,
	LoggingEvent
	}

import org.typelevel.log4cats.{
	LoggerFactory,
	SelfAwareStructuredLogger
	}


/**
 * The '''MockLoggerFactory''' type defines a
 * [[org.typelevel.log4cats.LoggerFactory]] which retains __all__ log events
 * produced by __any__ [[org.typelevel.log4cats.Logger]].  The primary purpose
 * of this type is to be able to verify the emission of specific
 * [[org.slf4j.event.LoggingEvent]]s programmatically.
 */
final case class MockLoggerFactory[F[_]] ()
	(implicit private val sync : Sync[F])
	extends LoggerFactory[F] ()
		with Iterable[LoggingEvent]
{
	/// Class Imports
	import MockLoggerFactory.MockLogger


	/// Instance Properties
	private val events = ListBuffer.empty[LoggingEvent]


	override def fromName (name : String) : F[SelfAwareStructuredLogger[F]] =
		sync.pure (new MockLogger[F] (events))


	override def getLoggerFromName (name : String)
		: SelfAwareStructuredLogger[F] =
		new MockLogger[F] (events)


	override def iterator : Iterator[LoggingEvent] =
		events.synchronized (events.toList.iterator)


	def clear () : Unit = events.synchronized (events.clear ())


	def infoOrAbove () : Iterator[LoggingEvent] =
		iterator.filter (_.getLevel.compareTo (Level.INFO) >= 0)
}


object MockLoggerFactory
{
	/// Class Types
	final class MockLogger[F[_]] (
		private val events : ListBuffer[LoggingEvent]
		)
		(implicit private val sync : Sync[F])
		extends SelfAwareStructuredLogger[F]
	{
		/// Instance Properties
		override val isTraceEnabled : F[Boolean] = sync.pure (true)
		override val isDebugEnabled : F[Boolean] = sync.pure (true)
		override val isInfoEnabled : F[Boolean] = sync.pure (true)
		override val isWarnEnabled : F[Boolean] = sync.pure (true)
		override val isErrorEnabled : F[Boolean] = sync.pure (true)

		private val logger = Slf4jLoggerFactory.getLogger (getClass)


		override def trace (ctx : Map[String, String])
			(msg : => String)
			: F[Unit] =
			addEvent (Level.TRACE, msg)


		override def trace (ctx : Map[String, String], t : Throwable)
			(msg: => String)
			: F[Unit] =
			addEvent (Level.TRACE, msg)


		override def debug (ctx : Map[String, String])
			(msg : => String)
			: F[Unit] =
			addEvent (Level.DEBUG, msg)


		override def debug (ctx : Map[String, String], t : Throwable)
			(msg : => String)
			: F[Unit] =
			addEvent (Level.DEBUG, msg)


		override def info (ctx : Map[String, String])
			(msg : => String)
			: F[Unit] =
			addEvent (Level.INFO, msg)


		override def info (ctx : Map[String, String], t : Throwable)
			(msg : => String)
			: F[Unit] =
			addEvent (Level.INFO, msg)


		override def warn (ctx : Map[String, String])
			(msg : => String)
			: F[Unit] =
			addEvent (Level.WARN, msg)


		override def warn (ctx : Map[String, String], t : Throwable)
			(msg : => String)
			: F[Unit] =
			addEvent (Level.WARN, msg)


		override def error (ctx : Map[String, String])
			(msg : => String)
			: F[Unit] =
			addEvent (Level.ERROR, msg)


		override def error (ctx : Map[String, String], t : Throwable)
			(msg : => String)
			: F[Unit] =
			addEvent (Level.ERROR, msg)


		override def error (t : Throwable)
			(message : => String)
			: F[Unit] =
			addEvent (Level.ERROR, message)


		override def warn (t : Throwable)
			(message : => String)
			: F[Unit] =
			addEvent (Level.WARN, message)


		override def info (t : Throwable)
			(message : => String)
			: F[Unit] =
			addEvent (Level.INFO, message)


		override def debug (t : Throwable)
			(message : => String)
			: F[Unit] =
			addEvent (Level.DEBUG, message)


		override def trace (t : Throwable)
			(message : => String)
			: F[Unit] =
			addEvent (Level.TRACE, message)


		override def error (message : => String) : F[Unit] =
			addEvent (Level.ERROR, message)


		override def warn (message : => String) : F[Unit] =
			addEvent (Level.WARN, message)


		override def info (message : => String) : F[Unit] =
			addEvent (Level.INFO, message)


		override def debug (message : => String) : F[Unit] =
			addEvent (Level.DEBUG, message)


		override def trace (message : => String) : F[Unit] =
			addEvent (Level.TRACE, message)


		private def addEvent (level : Level, message : String) : F[Unit] =
		sync.delay {
			val event = new DefaultLoggingEvent (level, logger)

			event.setMessage (message)
			events.synchronized (events.addOne (event))
			}
	}
}

