package com.github.osxhacker.demo.chassis.adapter.rest

import java.net.URI

import cats.{
	Eq,
	Show
	}


/**
 * The '''Relation''' type defines the reification of the
 * [[https://tools.ietf.org/html/rfc5988#section-4 Link Relation]] concept.
 * For an up-to-date listing of the IANA registered relations, including their
 * descriptions and defining authorities, see
 * [[https://www.iana.org/assignments/link-relations/link-relations.xml here]].
 */
sealed class Relation (private val name : String)
	extends Product1[String]
		with Serializable
{
	/// Class Imports
	import cats.syntax.eq._
	import mouse.boolean._


	/// Instance Properties
	final override val _1 : String = name
	final override val productPrefix : String = "Relation"


	final override def canEqual (that : Any) : Boolean =
		that.isInstanceOf[Relation]


	final override def equals (that : Any) : Boolean =
		canEqual (that) && _1.equals (that.asInstanceOf[Relation]._1)


	final override def hashCode () : Int = _1.hashCode ()


	final override def productElementName (n : Int) : String =
		(n === 0).fold ("name", super.productElementName (n))


	final override def toString () : String = s"$productPrefix(${_1})"
}


object Relation
{
	/// Class Imports
	import cats.syntax.option._
	import cats.syntax.show._


	/// Class Types
	case object About
		extends Relation ("about")

	case object Alternate
		extends Relation ("alternate")

	case object Appendix
		extends Relation ("appendix")

	case object Archives
		extends Relation ("archives")

	case object Author
		extends Relation ("author")

	case object Bookmark
		extends Relation ("bookmark")

	/**
	 * Non-standard link relation, as defined
	 * [[https://www.iana.org/assignments/link-relations/link-relations.xml here]].
	 */
	case object Cancel
		extends Relation ("cancel")

	case object Canonical
		extends Relation ("canonical")

	case object Chapter
		extends Relation ("chapter")

	case object Collection
		extends Relation ("collection")

	case object Contents
		extends Relation ("contents")

	case object Copyright
		extends Relation ("copyright")

	case object CreateForm
		extends Relation ("create-form")

	case object Current
		extends Relation ("current")

	/**
	 * Non-standard link relation, as defined
	 * [[https://www.iana.org/assignments/link-relations/link-relations.xml here]].
	 */
	case object Delete
		extends Relation ("delete")

	case object DescribedBy
		extends Relation ("describedby")

	case object Describes
		extends Relation ("describes")

	case object Disclosure
		extends Relation ("disclosure")

	case object Duplicate
		extends Relation ("duplicate")

	case object Edit
		extends Relation ("edit")

	case object EditForm
		extends Relation ("edit-form")

	case object EditMedia
		extends Relation ("edit-media")

	case object Enclosure
		extends Relation ("enclosure")

	case object First
		extends Relation ("first")

	case object Glossary
		extends Relation ("glossary")

	case object Help
		extends Relation ("help")

	case object Hosts
		extends Relation ("hosts")

	case object Hub
		extends Relation ("hub")

	case object Icon
		extends Relation ("icon")

	case object Index
		extends Relation ("index")

	case object Item
		extends Relation ("item")

	case object Last
		extends Relation ("last")

	case object LatestVersion
		extends Relation ("latest-version")

	case object License
		extends Relation ("license")

	case object Lrdd
		extends Relation ("lrdd")

	case object Monitor
		extends Relation ("monitor")

	case object MonitorGroup
		extends Relation ("monitor-group")

	case object Next
		extends Relation ("next")

	case object NextArchive
		extends Relation ("next-archive")

	case object NoFollow
		extends Relation ("nofollow")

	case object NoReferrer
		extends Relation ("noreferrer")

	case object Payment
		extends Relation ("payment")

	case object PredecessorVersion
		extends Relation ("predecessor-version")

	case object Prefetch
		extends Relation ("prefetch")

	case object Previous
		extends Relation ("prev")

	case object Preview
		extends Relation ("preview")

	case object PreviousArchive
		extends Relation ("prev-archive")

	case object PrivacyPolicy
		extends Relation ("privacy-policy")

	case object Profile
		extends Relation ("profile")

	case object Related
		extends Relation ("related")

	case object Replies
		extends Relation ("replies")

	case object Search
		extends Relation ("search")

	case object Section
		extends Relation ("section")

	case object Self
		extends Relation ("self")

	case object Service
		extends Relation ("service")

	case object Start
		extends Relation ("start")

	case object Stylesheet
		extends Relation ("stylesheet")

	case object Subsection
		extends Relation ("subsection")

	case object SuccessorVersion
		extends Relation ("successor-version")

	case object Tag
		extends Relation ("tag")

	case object TermsOfService
		extends Relation ("terms-of-service")

	case object Type
		extends Relation ("type")

	case object Up
		extends Relation ("up")

	case object VersionHistory
		extends Relation ("version-history")

	case object Via
		extends Relation ("via")

	case object WorkingCopy
		extends Relation ("working-copy")

	case object WorkingCopyOf
		extends Relation ("working-copy-of")


	/**
	 * This apply method is provided to support functional-style creation of a
	 * '''Relation''' which is not one of the `case object`s pre-defined here
	 * using an '''unregistered''' [[java.net.URI]].
	 */
	def apply (unregistered : URI) : Relation =
		new Relation (unregistered.toString ())


	/**
	 * The unapply method is provided to support pattern matching.  Note that
	 * the `case object`s defined in this type __also__ can be used in pattern
	 * matching, just for their specific type of course.
	 */
	def unapply (that : Any) : Option[String] =
		that match {
			case relation : Relation =>
				relation._1
					.some

			case _ =>
				none[String]
			}


	/// Implicit Conversions
	implicit val relationEq : Eq[Relation] = Eq.by (_._1)
	implicit val relationShow : Show[Relation] = Show.show (_._1)
}

