package com.github.osxhacker.demo.gatling.service.storageFacility

import io.circe.Json
import monocle._

import com.github.osxhacker.demo.api.storageFacility._


/**
 * The '''ResourceOptics''' type defines convenience [[monocle]] optics for use
 * with [[com.github.osxhacker.demo.api.storageFacility]] API types.
 */
trait ResourceOptics
{
	/// Class Types
	object storageFacilities
		extends StorageFacilities.Optics
	{
		/// Instance Properties
		lazy val self : Getter[StorageFacilities, Option[LinkObject]] =
			defineGetterForSemanticLink (semanticLinks) (_.self)

		lazy val semanticLinks
			: Optional[StorageFacilities, Links.AdditionalProperties] =
			_links.some
				.andThen (Links.Optics.additionalProperties)
				.some
	}


	object storageFacility
		extends StorageFacility.Optics
	{
		/// Instance Properties
		lazy val activate : Getter[StorageFacility, Option[LinkObject]] =
			defineGetterForSemanticLink (semanticLinks) (
				_.`urn:storage-facility:activate`
				)

		lazy val close : Getter[StorageFacility, Option[LinkObject]] =
			defineGetterForSemanticLink (semanticLinks) (
				_.`urn:storage-facility:close`
				)

		lazy val delete : Getter[StorageFacility, Option[LinkObject]] =
			defineGetterForSemanticLink (semanticLinks) (_.delete)

		lazy val edit : Getter[StorageFacility, Option[LinkObject]] =
			defineGetterForSemanticLink (semanticLinks) (_.edit)

		lazy val self : Getter[StorageFacility, Option[LinkObject]] =
			defineGetterForSemanticLink (semanticLinks) (_.self)

		lazy val semanticLinks
			: Optional[StorageFacility, Links.AdditionalProperties] =
			_links.some
				.andThen (Links.Optics.additionalProperties)
				.some
	}


	/**
	 * The hrefFor method is a convenience method for defining a functor capable
	 * of resolving the `href` within a
	 * [[com.github.osxhacker.demo.api.storageFacility.LinkObject]] iff there is
	 * one within an instance of ''SourceT''.
	 */
	protected def hrefFor[SourceT] (
		getter : Getter[SourceT, Option[LinkObject]]
		)
		: SourceT => Option[String] =
		getter
			.some
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

