package com.github.osxhacker.demo.storageFacility.domain.specification

import eu.timepit.refined.api.Refined
import monocle.Getter

import com.github.osxhacker.demo.chassis.domain.Specification
import com.github.osxhacker.demo.chassis.domain.event.Region


/**
 * The '''RegionIs''' type defines a
 * [[com.github.osxhacker.demo.chassis.domain.Specification]] which
 * `isSatisfiedBy` a ''SourceT'' having a
 * [[com.github.osxhacker.demo.chassis.domain.event.Region]]-like property
 * which is equivalent to the '''desired'''
 * [[com.github.osxhacker.demo.chassis.domain.event.Region]].
 */
final case class RegionIs[SourceT <: AnyRef, P] (
	private val desired : Region
	) (
	private val region : Getter[SourceT, Refined[String, P]]
)
	extends Specification[SourceT]
{
	/// Class Imports
	import cats.syntax.eq._


	/// Instance Properties
	private val thisRegion = Region.value
		.get (desired)
		.value


	override def apply (candidate : SourceT) : Boolean =
		thisRegion === region.get (candidate)
			.value


	override def toString () : String = "specification: region is"
}

