package com.github.osxhacker.demo.storageFacility.domain

import cats.{
	Eq,
	Show
	}

import cats.data.Ior
import com.softwaremill.diffx.Diff
import monocle.Optional

import com.github.osxhacker.demo.chassis.domain.Slug
import com.github.osxhacker.demo.chassis.domain.entity.Identifier


/**
 * The '''CompanyReference''' type is the Domain Object Model reification of a
 * symbolic reference to a
 * [[com.github.osxhacker.demo.storageFacility.domain.Company]].  There are two
 * ways a [[com.github.osxhacker.demo.storageFacility.domain.Company]] can be
 * identified:
 *
 *   1. Its [[com.github.osxhacker.demo.chassis.domain.entity.Identifier]]
 *
 *   1. Its [[com.github.osxhacker.demo.chassis.domain.Slug]]
 *
 * The [[cats.data.Ior]] `right` is preferred and its value is used when
 * possible.
 */
final case class CompanyReference (
	private val slugOrId : Ior[Slug, Identifier[Company]]
	)
{
	/// Class Imports
	import Function.const
	import cats.syntax.eq._


	override def equals (obj : Any) : Boolean =
		canEqual (obj) && {
			obj.asInstanceOf[CompanyReference].slugOrId match {
				case Ior.Left (slug) =>
					slugOrId.fold (
						_ === slug,
						const (false),
						(a, _) => a === slug
						)

				case Ior.Right (id) =>
					slugOrId.fold (const (false), _ === id, (_, b) => b === id)

				case Ior.Both (slug, id) =>
					slugOrId.fold (_ === slug, _ === id, (_, b) => b === id)
				}
			}


	override def hashCode () : Int = fold (_.hashCode (), _.hashCode ())


	/**
	 * The fold method defines a
	 * [[https://en.wikipedia.org/wiki/Catamorphism catamorphism]] which reduces
	 * the [[com.github.osxhacker.demo.chassis.domain.Slug]] or
	 * [[com.github.osxhacker.demo.chassis.domain.entity.Identifier]] contained
	 * in `slugOrId` to a unified type ''A''.  When both are present, the
	 * [[com.github.osxhacker.demo.chassis.domain.entity.Identifier]] is used.
	 */
	def fold[A] (whenSlug : Slug => A, whenId : Identifier[Company] => A) : A =
		slugOrId.fold (whenSlug, whenId, (_, b) => whenId (b))
}


object CompanyReference
{
	/// Class Imports
	import cats.syntax.show._


	/// Instance Properties
	val id = Optional[CompanyReference, Identifier[Company]] (_.slugOrId.right) (
		v => cr => CompanyReference (cr.slugOrId.putRight (v))
		)

	val slug = Optional[CompanyReference, Slug] (_.slugOrId.left) (
		v => cr => CompanyReference (cr.slugOrId.putLeft (v))
		)


	/**
	 * This version of the apply method is provided to allow functional-style
	 * creation of a '''CompanyReference''' given an '''id'''.
	 */
	def apply (id : Identifier[Company]) : CompanyReference =
		CompanyReference (Ior.right (id))


	/**
	 * This version of the apply method is provided to allow functional-style
	 * creation of a '''CompanyReference''' given a '''slug'''.
	 */
	def apply (slug : Slug) : CompanyReference =
		CompanyReference (Ior.left (slug))


	/**
	 * This version of the apply method is provided to allow functional-style
	 * creation of a '''CompanyReference''' given both an '''id''' __and__
	 * a '''slug'''.
	 */
	def apply (slug : Slug, id : Identifier[Company]) : CompanyReference =
		CompanyReference (Ior.both (slug, id))


	/// Implicit Conversions
	implicit val companyReferenceDiff : Diff[CompanyReference] = Diff.useEquals
	implicit val companyReferenceEq : Eq[CompanyReference] =
		Eq.fromUniversalEquals

	implicit val companyReferenceShow : Show[CompanyReference] =
		Show.show (_.slugOrId.fold (_.show, _.show, (a, b) => (a, b).show))
}

