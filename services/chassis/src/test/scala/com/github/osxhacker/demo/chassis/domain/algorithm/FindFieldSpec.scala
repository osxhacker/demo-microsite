package com.github.osxhacker.demo.chassis.domain.algorithm

import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec


/**
 * The '''FindFieldSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.domain.entity.Identifier]] for fitness of
 * purpose and serves as an exemplar of its use.
 */
final class FindFieldSpec ()
	extends AnyWordSpec
		with Diagrams
{
	/// Class Imports
	import FindFieldSpec._


	/// Instance Properties
	private val desired = Desired ("hello, world", 42)
	private val nested = Nested (1.23, desired)
	private val wrapped = Wrapped (desired)


	"The FindField algorithm" must {
		"be able to find a field in the top-most type" in {
			val result = FindField[Desired, String] (desired, Symbol ("string"))

			assert (result === desired.string)
			}

		"be able to find a field when it is wrapped" in {
			val result = FindField[Wrapped, Int] (wrapped, Symbol ("int"))

			assert (result === desired.int)
			}

		"be able to find a field in a wrapped type with multiple fields" in {
			val result = FindField[Nested, Int] (nested, Symbol ("int"))

			assert (result === desired.int)
			}

		"be able to find a field inside a tuple (first field)" in {
			val result = FindField[(Desired, Int), String] (
				desired -> 99,
				Symbol ("string")
				)

			assert (result === desired.string)
			}

		"be able to find a field inside a tuple (second field)" in {
			val result = FindField[(Int, Desired), String] (
				99 -> desired,
				Symbol ("string")
				)

			assert (result === desired.string)
			}

		"be able to find a field nested inside a tuple (first field)" in {
			val result = FindField[(Nested, Int), Int] (
				nested -> 123,
				Symbol ("int")
				)

			assert (result === desired.int)
			}

		"be able to find a field nested inside a tuple (second field)" in {
			val result = FindField[(Int, Nested), Int] (
				123 -> nested,
				Symbol ("int")
				)

			assert (result === desired.int)
			}

		"fail to compile with an unknown field" in {
			assertDoesNotCompile (
				"""
				FindField[(String, Double, Nested), String] (
					("a", 1.0, nested),
					Symbol ("unknown")
					)
				"""
				)
			}
		}
}


object FindFieldSpec
{
	/// Class Types
	final case class Desired (string : String, int : Int)


	final case class Nested (a : Double, desired : Desired)


	final case class Wrapped (desired : Desired)
}

