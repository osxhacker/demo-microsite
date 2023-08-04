package com.github.osxhacker.demo.chassis.adapter

import com.monovore.decline._
import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec


/**
 * The '''ProgramArgumentsSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.adapter.ProgramArguments]] for fitness
 * of purpose and serves as an exemplar of its use.
 */
final class ProgramArgumentsSpec ()
	extends AnyWordSpec
		with Diagrams
{
	/// Class Imports
	import ProgramArguments.{
		DockerInvocation,
		NativeInvocation
		}


	/// Class Types
	final case class SampleParser (val enableHelpFlag : Boolean = true)
		extends ProgramArguments
	{
		/// Instance Properties
		val parser = Command (
			name = "unit-test",
			header = "Representative program arguments use.",
			helpFlag = enableHelpFlag
			) (arguments ())


		def apply (parameters : Seq[String]): Either[Help, Product] =
			parser.parse (parameters)
	}


	"The ProgramArguments type" must {
		"be able to produce help text" in {
			val instance = SampleParser ()
			val help = instance (
				"--help" ::
				Nil
				)

			assert (help.isLeft)
			assert (help.swap.exists (_.toString.nonEmpty))
			}

		"be able to identify a docker invocation" in {
			val instance = SampleParser ()
			val parsed = instance (
				"--docker" ::
				"--plain" ::
				Nil
				)

			parsed match {
				case Right (DockerInvocation (settings, format, verbose)) =>
					assert (settings.value !== "application.conf")
					assert (format === "plain")
					assert (verbose === false)

				case other =>
					fail (s"unexpected result: $other")
				}
			}

		"be able to identify an explicit native invocation" in {
			val instance = SampleParser ()
			val parsed = instance (
				"--native" ::
				"--plain" ::
				Nil
			)

			parsed match {
				case Right (NativeInvocation (settings, format, verbose)) =>
					assert (settings.value === "application.conf")
					assert (format === "plain")
					assert (verbose === false)

				case other =>
					fail (s"unexpected result: $other")
				}
			}

		"default to a native invocation" in {
			val instance = SampleParser ()
			val parsed = instance (Nil)

			parsed match {
				case Right (NativeInvocation (settings, format, verbose)) =>
					assert (settings.value === "application.conf")
					assert (format === "json")
					assert (verbose === false)

				case other =>
					fail (s"unexpected result: $other")
				}
			}
		}
}
