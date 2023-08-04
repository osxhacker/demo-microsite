package com.github.osxhacker.demo.storageFacility.adapter.database

import shapeless.tag
import shapeless.tag.@@


/**
 * The '''PrimaryKey''' type reifies the concept of a primary database '''key'''
 * associated with a ''RecordT''.
 */
private[database] final class PrimaryKey[RecordT <: Product] (
	val key : Int @@ RecordT
	)
	extends AnyVal


object PrimaryKey
{
	/**
	 * The apply method is provided to support functional-style creation by
	 * [[shapeless.tag]]ging an `Int` '''key''' as belonging to the specified
	 * ''RecordT''.
	 */
	def apply[RecordT <: Product] (key : Int) : PrimaryKey[RecordT] =
		new PrimaryKey[RecordT] (tag[RecordT] (key))
}

