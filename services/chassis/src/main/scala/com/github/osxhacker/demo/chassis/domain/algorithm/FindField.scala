package com.github.osxhacker.demo.chassis.domain.algorithm

import scala.annotation.implicitNotFound

import shapeless.{
	syntax => _,
	_
	}

import shapeless.labelled.FieldType


/**
 * The '''FindField''' type defines a higher-kinded algorithm for locating a
 * property (of name ''K'') with the type ''V'' arbitrarily located within
 * tuples and/or `case class`es starting with ''A''.  A useful way to employ
 * '''FindField''' is to include it as an `implicit` and allow the compiler to
 * resolve it automatically.  For example:
 *
 * {{{
 *     def foo[A] ()
 *         (implicit findBar : FindField[A, Witness.`'bar`.T, TheTypeOfBar])
 * }}}
 *
 * Note that the use of [[shapeless.Witness]] above uses the deprecated "single
 * tick" [[scala.Symbol]] syntax.  This is done so that the [[scala.Symbol]] is
 * a compile-time constant, thus enabling Shapeless `macro`s to manufacture the
 * requisite instance.
 *
 * Conceptually, '''FindField''' abstracts a named field's location.  Another
 * way to think about it is as being a higher-kinded [[monacle.Getter]].
 */
trait FindField[A, K <: Symbol, V]
{
	def apply (instance : A) : V
}


object FindField
	extends LowPriorityFindImplicits
{
	/**
	 * The apply method exists to support "direct" use, as opposed to the more
	 * common `implicit` summoning described in
	 * [[com.github.osxhacker.demo.chassis.domain.algorithm.FindField]].
	 */
	def apply[A, V] (instance : A, name : Witness.Lt[Symbol])
		(
			implicit
			@implicitNotFound (
				"could not find desired named field of type ${V} in ${A}"
				)
			finder : FindField[A, name.T, V]
		)
		: V =
		finder (instance)


	/// Implicit Conversions
	implicit def fromProduct[A, HL <: HList, K <: Symbol, V] (
		implicit
		lgen : LabelledGeneric.Aux[A, HL],
		findField : Lazy[FindField[HL, K, V]]
		)
		: FindField[A, K, V] =
		createFinder (instance => findField.value (lgen.to (instance)))


	implicit def matches[K <: Symbol, V, TailT <: HList]
		: FindField[FieldType[K, V] :: TailT, K, V] =
		createFinder (_.head)


	implicit def recurse[H, K <: Symbol, TailT <: HList, V] (
		implicit
		findField : Lazy[FindField[H, K, V]]
		)
		: FindField[H :: TailT, K, V] =
		createFinder (hl => findField.value (hl.head))
}


sealed trait LowPriorityFindImplicits
{
	protected def createFinder[A, K <: Symbol, V] (f : A => V)
		: FindField[A, K, V] =
		new FindField[A, K, V] {
			override def apply (instance : A) : V = f (instance)
			}


	/// Implicit Conversions
	implicit def fromGeneric[A, HL <: HList, K <: Symbol, V] (
		implicit
		gen : Generic.Aux[A, HL],
		findField : Lazy[FindField[HL, K, V]]
		)
		: FindField[A, K, V] =
		createFinder (instance => findField.value (gen.to (instance)))


	implicit def keepSearching[H, TailT <: HList, K <: Symbol, V] (
		implicit findInTail : FindField[TailT, K, V]
		)
		: FindField[H :: TailT, K, V] =
		createFinder (hl => findInTail (hl.tail))
}

