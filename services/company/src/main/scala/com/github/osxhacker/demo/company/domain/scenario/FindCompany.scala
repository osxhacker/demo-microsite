package com.github.osxhacker.demo.company.domain.scenario

import cats.ApplicativeThrow
import monocle.{
	Getter,
	Iso
	}

import com.github.osxhacker.demo.chassis
import com.github.osxhacker.demo.chassis.domain.entity.Identifier
import com.github.osxhacker.demo.chassis.domain.error.ValidationError
import com.github.osxhacker.demo.chassis.effect.{
	Aspect,
	Pointcut
	}

import com.github.osxhacker.demo.chassis.monitoring.metrics.UseCaseScenario
import com.github.osxhacker.demo.company.domain.{
	Company,
	ScopedEnvironment
	}


/**
 * The '''FindCompany''' type defines the Use-Case scenario responsible for
 * retrieving a [[com.github.osxhacker.demo.company.domain.Company]] based
 * only on its
 * [[com.github.osxhacker.demo.chassis.domain.entity.Identifier]].
 */
final case class FindCompany[F[_], SourceT <: AnyRef] (
	private val id : AdaptOptics.KleisliType[SourceT, Identifier[Company]]
	)
	(
		implicit

		/// Needed for `raiseError`.
		private val applicativeThrow : ApplicativeThrow[F],

		/// Needed for `measure`.
		private val pointcut : Pointcut[F]
	)
{
	/// Class Imports
	import cats.syntax.applicativeError._
	import chassis.syntax._


	/// Instance Properties
	implicit lazy val cachedAspect = Aspect[
		F,
		UseCaseScenario[F, FindCompany[F, SourceT], Company]
		].static ()


	def apply (source : SourceT)
		(implicit env : ScopedEnvironment[F])
		: F[Company] =
		id (source).fold (
			ValidationError[Company] (_).raiseError[F, Company],
			env.companies.find
			)
			.measure[UseCaseScenario[F, FindCompany[F, SourceT], Company]] ()
}

object FindCompany
{
	/// Class Types
	final class PartiallyApplied[F[_]] ()
	{
		/**
		 * This version of the apply method constructs a '''FindCompany'''
		 * scenario which has as its ''SourceT'' the
		 * [[com.github.osxhacker.demo.chassis.domain.entity.Identifier]]
		 * required.
		 */
		def apply ()
			(
				implicit
				applicativeThrow : ApplicativeThrow[F],
				pointcut : Pointcut[F]
			)
			: FindCompany[F, Identifier[Company]] =
			new FindCompany[F, Identifier[Company]] (AdaptOptics (Iso.id))


		/**
		 * This version of the apply method constructs a '''FindCompany'''
		 * scenario which attempts to use an
		 * [[com.github.osxhacker.demo.chassis.domain.entity.Identifier]]
		 * within an arbitrary ''SourceT''.
		 */
		def apply[SourceT <: AnyRef, StatusT <: AnyRef, IdT] (
			id : Getter[SourceT, IdT]
			)
			(
				implicit
				applicativeThrow : ApplicativeThrow[F],
				parser : Identifier.Parser[Company, IdT],
				pointcut : Pointcut[F]
			)
			: FindCompany[F, SourceT] =
			new FindCompany[F, SourceT] (id = AdaptOptics.id (id))
	}


	/**
	 * The apply method is provided to support functional-style creation and
	 * employs the "partially applied" idiom, thus only requiring collaborators
	 * to provide ''F'' and allow the compiler to deduce the remaining type
	 * parameters.
	 */
	@inline
	def apply[F[_]] : PartiallyApplied[F] = new PartiallyApplied[F] ()
}

