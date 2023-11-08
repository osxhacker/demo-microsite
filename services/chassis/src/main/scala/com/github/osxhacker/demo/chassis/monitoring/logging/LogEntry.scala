package com.github.osxhacker.demo.chassis.monitoring.logging

import scala.util.Using

import cats.Show
import org.slf4j.Logger

import com.github.osxhacker.demo.chassis.monitoring.{
	CorrelationId,
	Subsystem
	}


/**
 * The '''LogEntry''' type defines the contract for a __pending__ log event
 * intended to be emitted effectually.
 */
sealed abstract class LogEntry (
	/// TODO: incorporate the subsystem property into context
	private val subsystem : Subsystem,
	private val message : String,
	private val cause : Option[Throwable],
	private val context : Map[String, String]
	)
	extends Product
		with Serializable
{
	/// Class Imports
	import mouse.option._


	/**
	 * The addContext method provides the ability to associate '''additional'''
	 * context to '''this''' '''LogEntry'''.
	 */
	def addContext (additional : Map[String, String]) : LogEntry


	/**
	 * The debug method `emit`s '''this''' instance iff
	 * [[org.slf4j.event.Level.DEBUG]] is enabled.
	 */
	final def debug ()
		(implicit logger : Logger)
		: Unit =
		if (logger.isDebugEnabled)
			Using (ScopedMDC (context)) {
				 emit (_.debug (message), problem => _.debug (message, problem))
				 }


	/**
	 * The error method `emit`s '''this''' instance as an
	 * [[org.slf4j.event.Level.ERROR]] logging event.
	 */
	final def error ()
		(implicit logger : Logger)
		: Unit =
		Using (ScopedMDC (context)) {
			emit (_.error (message), problem => _.error (message, problem))
			}


	/**
	 * The info method `emit`s '''this''' instance as an
	 * [[org.slf4j.event.Level.INFO]] logging event.
	 */
	final def info ()
		(implicit logger : Logger)
		: Unit =
		Using (ScopedMDC (context)) {
			emit (_.info (message), problem => _.info (message, problem))
			}


	/**
	 * The warn method `emit`s '''this''' instance as an
	 * [[org.slf4j.event.Level.WARN]] logging event.
	 */
	final def warn ()
		(implicit logger : Logger)
		: Unit =
		Using (ScopedMDC (context)) {
			emit (_.warn (message), problem => _.warn (message, problem))
			}


	@inline
	private def emit (
		withoutError : ScopedMDC => Unit,
		withError : Throwable => ScopedMDC => Unit
		)
		: ScopedMDC => Unit =
		cause.cata (withError, withoutError)
}


object LogEntry
{
	/// Class Imports
	import cats.syntax.option._


	/**
	 * This version of the apply method creates a
	 * [[com.github.osxhacker.demo.chassis.monitoring.logging.LogEntry]] with
	 * the given '''correlationId''', '''message''', and execution
	 * '''context'''.
	 */
	def apply (correlationId : CorrelationId, message : String)
		(implicit subsystem : Subsystem)
		: LogEntry =
		WorkflowLogEntry (
			correlationId,
			message,
			none[Throwable],
			Map.empty
			)


	/**
	 * This version of the apply method creates a
	 * [[com.github.osxhacker.demo.chassis.monitoring.logging.LogEntry]] in the
	 * presence of the '''cause''' of a problem.  The given '''correlationId''',
	 * '''message''', and execution '''context''' detail information known when
	 * the problem was encountered.
	 */
	def apply (
		correlationId : CorrelationId,
		message : String,
		cause : Throwable
		)
		(implicit subsystem : Subsystem)
		: LogEntry =
		WorkflowLogEntry (
			correlationId,
			message,
			cause.some,
			Map.empty
			)
}


/**
 * The '''SystemLogEntry''' type is the
 * [[com.github.osxhacker.demo.chassis.monitoring.logging.LogEntry]] available
 * when a pending log event is produced from logic which is __not__ directly a
 * part of a workflow or when a
 * [[com.github.osxhacker.demo.chassis.monitoring.logging.WorkflowLogEntry]]
 * cannot be made.
 */
final case class SystemLogEntry (
	private val message : String,
	private val cause : Option[Throwable],
	private val context : Map[String, String]
	)
	(implicit private val subsystem : Subsystem)
	extends LogEntry (subsystem, message, cause, context)
{
	override def addContext (additional : Map[String, String])
		: SystemLogEntry =
		copy (context = context ++ additional)
}


final case class WorkflowLogEntry (
	private val correlationId : CorrelationId,
	private val message : String,
	private val cause : Option[Throwable],
	private val context : Map[String, String]
	)
	(implicit private val subsystem : Subsystem)
	extends LogEntry (
		subsystem,
		message,
		cause,
		context.updated (
			"correlationId",
			Show[CorrelationId].show (correlationId)
			)
		)
{
	override def addContext (additional : Map[String, String])
		: WorkflowLogEntry =
		copy (context = context ++ additional)
}

