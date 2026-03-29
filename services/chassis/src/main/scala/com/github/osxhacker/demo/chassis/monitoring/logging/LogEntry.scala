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
	override val message : String,
	override val cause : Option[Throwable],
	private val subsystem : Subsystem,
	private val entries : Map[String, String]
	)
	extends Product
		with Serializable
		with Loggable
{
	/// Class Imports
	import mouse.option._


	/// Instance Properties
	override lazy val context = subsystem addTo entries


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
		withoutCause : ScopedMDC => Unit,
		withCause : Throwable => ScopedMDC => Unit
		)
		: ScopedMDC => Unit =
		cause.cata (withCause, withoutCause)
}


object LogEntry
{
	/// Class Imports
	import cats.syntax.option._


	/**
	 * This version of the apply method creates a
	 * [[com.github.osxhacker.demo.chassis.monitoring.logging.LogEntry]] with
	 * the given '''correlationId''', '''message''', and an empty execution
	 * '''context'''.
	 */
	def apply (correlationId : CorrelationId, message : String)
		(implicit subsystem : Subsystem)
		: LogEntry =
		WorkflowLogEntry (
			message,
			none[Throwable],
			correlationId,
			Map.empty
			)


	/**
	 * This version of the apply method creates a
	 * [[com.github.osxhacker.demo.chassis.monitoring.logging.LogEntry]] in the
	 * presence of the '''cause''' of a problem.  The given '''correlationId''',
	 * '''message''', and an empty execution '''context''' detail information
	 * known when the problem was encountered.
	 */
	def apply (
		correlationId : CorrelationId,
		message : String,
		cause : Throwable
		)
		(implicit subsystem : Subsystem)
		: LogEntry =
		WorkflowLogEntry (
			message,
			Option (cause),
			correlationId,
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
	override val message : String,
	override val cause : Option[Throwable],
	private val entries : Map[String, String]
	)
	(implicit private val subsystem : Subsystem)
	extends LogEntry (message, cause, subsystem, entries)
{
	override def addContext (additional : Map[String, String])
		: SystemLogEntry =
		copy (entries = context ++ additional)
}


/**
 * The '''WorkflowLogEntry''' type is the default
 * [[com.github.osxhacker.demo.chassis.monitoring.logging.LogEntry]]
 * implementation used when an application is performing a specific Use-Case
 * scenario.  As such, each __must__ have a
 * [[com.github.osxhacker.demo.chassis.monitoring.CorrelationId]] to identify
 * same in addition to the requirements of other
 * [[com.github.osxhacker.demo.chassis.monitoring.logging.LogEntry]] types.
 */
final case class WorkflowLogEntry (
	override val message : String,
	override val cause : Option[Throwable],
	private val correlationId : CorrelationId,
	private val entries : Map[String, String]
	)
	(implicit private val subsystem : Subsystem)
	extends LogEntry (
		message,
		cause,
		subsystem,
		entries.updated (
			"correlationId",
			Show[CorrelationId].show (correlationId)
			)
		)
{
	override def addContext (additional : Map[String, String])
		: WorkflowLogEntry =
		copy (entries = context ++ additional)
}

