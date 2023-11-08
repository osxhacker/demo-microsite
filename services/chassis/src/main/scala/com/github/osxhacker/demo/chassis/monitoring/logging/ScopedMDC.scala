package com.github.osxhacker.demo.chassis.monitoring.logging

import java.util.{
	Map => JMap
	}

import scala.jdk.CollectionConverters._
import scala.language.implicitConversions
import scala.util.Using.Releasable

import org.slf4j.{
	Logger,
	MDC
	}


/**
 * The '''ScopedMDC''' type establishes the
 * [[https://www.slf4j.org/manual.html#mdc SLF4j MDC]] for a __single__
 * interaction with a [[org.slf4j.Logger]].  It is designed to work seamlessly
 * with the [[scala.util.Using]] type.  For example:
 *
 * {{{
 *     implicit val logger : Logger = ...
 *     val addEntries = Map ("aKey" -> "value", "another" -> "example")
 *
 *     Using (ScopedMDC (addEntries)) {
 *         _.debug ("hello, world!")
 *         }
 * }}}
 *
 * In this example, `addEntries` will be __added__ to any existing `MDC`
 * entries for the duration of the enclosing block.  In all cases, the thread's
 * `MDC` is restored to its previous contents (when used as illustrated above).
 */
final case class ScopedMDC (private val entries : Map[String, String])
	(implicit private val logger : Logger)
{
	/// Class Imports
	import mouse.option._


	/// Instance Properties
	private[logging] val priorContext : Option[JMap[String, String]] =
		Option (MDC.getCopyOfContextMap)


	/// Constructor Body
	MDC.setContextMap (
		(
			priorContext.cata (_.asScala.toMap, Map.empty[String, String]) ++
			entries
		).asJava
		)
}


object ScopedMDC
{
	/// Implicit Conversions
	implicit val scopedMDCReleasable = new Releasable[ScopedMDC] {
		override def release (resource : ScopedMDC) : Unit =
			MDC.setContextMap (resource.priorContext.orNull)
		}


	implicit def scopedMDCToLogger (scopedMDC : ScopedMDC) : Logger =
		scopedMDC.logger
}

