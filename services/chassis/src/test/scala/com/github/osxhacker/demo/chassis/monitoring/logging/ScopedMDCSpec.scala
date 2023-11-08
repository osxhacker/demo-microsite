package com.github.osxhacker.demo.chassis.monitoring.logging

import scala.util.Using

import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.{
	Logger,
	MDC
	}

import org.slf4j.helpers.NOPLogger

import com.github.osxhacker.demo.chassis.ProjectSpec


/**
 * The '''ScopedMDCSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.monitoring.logging.ScopedMDC]] for
 * fitness of purpose and serves as an exemplar of its use.
 */
final class ScopedMDCSpec ()
	extends AnyWordSpec
		with ProjectSpec
		with Diagrams
{
	/// Instance Properties
	implicit private val logger : Logger = NOPLogger.NOP_LOGGER


	"The ScopedMDC type" must {
		"establish an MDC when one does not exist" in {
			val addEntries = Map ("aKey" -> "its value")

			assert (MDC.getCopyOfContextMap eq null)

			Using (ScopedMDC (addEntries)) {
				_ =>
					assert (MDC.get ("aKey") === "its value")
				 }

			assert (MDC.getCopyOfContextMap eq null)
			}

		"add to an MDC which already has entries" in {
			val addEntries = Map ("added" -> "hello, world!")

			assert (MDC.getCopyOfContextMap eq null)

			MDC.put ("existing", "added before scoped mdc use")
			assert (MDC.getCopyOfContextMap ne null)
			assert (MDC.get ("existing").nonEmpty)
			assert (MDC.get ("added") eq null)

			Using (ScopedMDC (addEntries)) {
				_ =>
					assert (MDC.get ("existing") ne null)
					assert (MDC.get ("added") ne null)
					assert (MDC.get ("added") === addEntries ("added"))
				 }

			assert (MDC.getCopyOfContextMap ne null)
			assert (MDC.get ("existing").nonEmpty)
			assert (MDC.get ("added") eq null)
			}
		}
}

