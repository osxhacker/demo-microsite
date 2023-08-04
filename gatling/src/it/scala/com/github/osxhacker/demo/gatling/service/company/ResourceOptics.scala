package com.github.osxhacker.demo.gatling.service.company

import io.circe.Json
import monocle._

import com.github.osxhacker.demo.api.company._


/**
 * The '''ResourceOptics''' type defines convenience [[monocle]] optics for use
 * with [[com.github.osxhacker.demo.api.company]] API types.
 */
trait ResourceOptics
{
	/// Class Types
	object companies
		extends Companies.Optics
	{
		/// Instance Properties
		lazy val findBySlug
			: String => Getter[Vector[Company], Option[Company]] =
			candidate => Getter (_.find (_.slug == candidate))

		lazy val self : Getter[Companies, Option[LinkObject]] =
			defineGetterForSemanticLink (semanticLinks) (_.self)

		lazy val semanticLinks
			: Optional[Companies, Links.AdditionalProperties] =
			_links.some
				.andThen (Links.Optics.additionalProperties)
				.some
	}


	object company
		extends Company.Optics
	{
		/// Instance Properties
		lazy val delete : Getter[Company, Option[LinkObject]] =
			defineGetterForSemanticLink (semanticLinks) (_.delete)

		lazy val edit : Getter[Company, Option[LinkObject]] =
			defineGetterForSemanticLink (semanticLinks) (_.edit)

		lazy val self : Getter[Company, Option[LinkObject]] =
			defineGetterForSemanticLink (semanticLinks) (_.self)

		lazy val semanticLinks : Optional[Company, Links.AdditionalProperties] =
			_links.some
				.andThen (Links.Optics.additionalProperties)
				.some
	}


	/**
	 * The hrefFor method is a convenience method for defining a functor capable
	 * of resolving the `href` within a
	 * [[com.github.osxhacker.demo.api.company.LinkObject]] iff there is one
	 * within an instance of ''SourceT''.
	 */
	protected def hrefFor[SourceT] (
		getter : Getter[SourceT, Option[LinkObject]]
		)
		: SourceT => Option[String] =
		getter.some
			.andThen (LinkObject.Optics.href)
			.headOption


	private def defineGetterForSemanticLink[SourceT] (
		links : Optional[SourceT, Links.AdditionalProperties]
		)
		(entry : Links.AdditionalProperties => Option[Json])
		: Getter[SourceT, Option[LinkObject]] =
		Getter {
			links.to (entry)
				.headOption (_)
				.flatten
				.flatMap (_.as[LinkObject].toOption)
			}
}


object ResourceOptics
	extends ResourceOptics

