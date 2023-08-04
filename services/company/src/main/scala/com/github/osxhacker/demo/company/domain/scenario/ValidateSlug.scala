package com.github.osxhacker.demo.company.domain.scenario

import cats.data._

import com.github.osxhacker.demo.chassis.domain.Slug
import com.github.osxhacker.demo.company.domain.ScopedEnvironment


/**
 * The '''ValidateSlug''' `object` defines the algorithm for ensuring a
 * well-formed [[com.github.osxhacker.demo.chassis.domain.Slug]] can be used in
 * the definition of a [[com.github.osxhacker.demo.company.domain.Company]].
 */
private[scenario] object ValidateSlug
{
	/// Class Imports
	import cats.syntax.eq._
	import cats.syntax.show._


	/// Class Types
	type ResultType[+A] = ValidatedNec[String, A]


	def apply[SourceT, F[_]] (
		source : SourceT,
		slug : Kleisli[ResultType, SourceT, Slug]
		)
		(implicit env : ScopedEnvironment[F])
		: ResultType[Slug] =
		slug (source) andThen validate (env.reservedSlugs)


	private def validate (reserved : NonEmptySet[Slug])
		(candidate : Slug)
		: ResultType[Slug] =
		Validated.condNec (
			!reserved.contains (candidate),
			candidate,
			s"cannot use a reserved slug: '${candidate.show}'"
			)
}

