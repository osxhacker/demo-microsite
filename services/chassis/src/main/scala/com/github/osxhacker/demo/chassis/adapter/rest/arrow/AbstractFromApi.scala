package com.github.osxhacker.demo.chassis.adapter.rest.arrow

import scala.language.postfixOps

import cats.Endo
import cats.arrow.{
	Arrow,
	FunctionK
	}


/**
 * The '''AbstractFromApi''' type defines the workflow for producing a
 * ''DomainT'' from its related ''ApiT''.  It is made `abstract` so that
 * service-specific abstractions can provide a partially bound ''ArrowT''
 * definition specific to their collaborations.
 *
 * The steps employed are, in order:
 *
 *   - Optionally `prepare` an ''ApiT'' instance (defaults to `identity`).
 *
 *   - Transform an ''ApiT'' instance into ''F[DomainT]''.
 */
abstract class AbstractFromApi[ArrowT[_, _], F[+_], ApiT, DomainT] ()
	(
		implicit
		/// Needed for `>>>` and `lift`.
		private val arrow : Arrow[ArrowT],

		/// Needed to transform `factory` into an `ArrowT`.
		private val transformerToArrowT : FunctionK[
			Lambda[A => ApiT => F[A]],
			ArrowT[ApiT, *]
			]
	)
{
	/// Class Imports
	import cats.syntax.compose._


	/// Instance Properties
	protected def prepare : Endo[ApiT] = identity
	protected def factory : ApiT => F[DomainT]

	private lazy val steps =
		arrow.lift (prepare) >>>
		transformerToArrowT (factory)


	/**
	 * The apply method is an alias for `run`.
	 */
	final def apply () : ArrowT[ApiT, DomainT] = run ()


	/**
	 * The run method produces an ''ArrowT'' which has __all__ steps needed to
	 * produce a ''DomainT'' from an arbitrary ''ApiT''.
	 */
	final def run () : ArrowT[ApiT, DomainT] = steps
}

