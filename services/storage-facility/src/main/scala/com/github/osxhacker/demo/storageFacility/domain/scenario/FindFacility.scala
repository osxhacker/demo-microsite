package com.github.osxhacker.demo.storageFacility.domain.scenario

import scala.language.postfixOps

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
import com.github.osxhacker.demo.storageFacility.domain.{
	ScopedEnvironment,
	StorageFacility
	}


/**
 * The '''FindFacility''' type defines the Use-Case scenario responsible for
 * retrieving a
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]] based
 * only on its
 * [[com.github.osxhacker.demo.chassis.domain.entity.Identifier]].  If it does
 * not exist or there is a problem retrieving it, an error is raised in ''F''.
 */
final class FindFacility[F[_], SourceT <: AnyRef] private (
	private val id : AdaptOptics.KleisliType[SourceT, Identifier[StorageFacility]]
	)
	(
		implicit

		/// Needed for `raiseError`.
		private val applicativeThrow : ApplicativeThrow[F],

		/// Needed for `Aspect`.
		private val pointcut : Pointcut[F]
	)
{
	/// Class Imports
	import cats.syntax.applicativeError._
	import chassis.syntax._


	/// Instance Properties
	implicit lazy val cachedAspect = Aspect[
		F,
		UseCaseScenario[
			F,
			FindFacility[F, SourceT],
			StorageFacility
			]
		].static ()


	override def toString () : String = "scenario: find facility"


	def apply (source : SourceT)
		(implicit env : ScopedEnvironment[F])
		: F[StorageFacility] =
		id (source).fold (
			ValidationError[StorageFacility] (_).raiseError[F, StorageFacility],
			env.storageFacilities.find
			)
			.measure[
				UseCaseScenario[
					F,
					FindFacility[F, SourceT],
					StorageFacility
					]
				] ()
}


object FindFacility
{
	/// Class Types
	final class PartiallyApplied[F[_]] ()
	{
		/**
		 * This version of the apply method constructs a '''FindFacility'''
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
			: FindFacility[F, Identifier[StorageFacility]] =
			new FindFacility[F, Identifier[StorageFacility]] (
				AdaptOptics.id (Iso.id)
				)


		/**
		 * This version of the apply method constructs a '''FindFacility'''
		 * scenario which attempts to use an
		 * [[com.github.osxhacker.demo.chassis.domain.entity.Identifier]]
		 * within an arbitrary ''SourceT''.
		 */
		def apply[SourceT <: AnyRef, IdT] (id : Getter[SourceT, IdT])
			(
				implicit
				applicativeThrow : ApplicativeThrow[F],
				parser : Identifier.Parser[StorageFacility, IdT],
				pointcut : Pointcut[F]
			)
			: FindFacility[F, SourceT] =
			new FindFacility[F, SourceT] (id = AdaptOptics.id (id))
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

