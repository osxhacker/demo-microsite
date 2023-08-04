package com.github.osxhacker.demo.chassis.domain.algorithm

import scala.annotation.{
	implicitNotFound,
	unused
	}

import cats.data.Chain
import shapeless.{
	syntax => _,
	_
	}

import shapeless.ops.{
	coproduct,
	hlist
	}


/**
 * The '''InferDomainEvents''' type defines the algorithm for determining, or
 * "inferring", what ''AllowableEventsT'' should be produced.  What ones created
 * are the responsibility of the ''InferPoly'' [[shapeless.Poly2]].  Its
 * `implicit` signatures must be of the form:
 *
 * {{{
 *     object Sample
 *         extends Poly2
 *     {
 *         implicit val caseSomeEvent
 *             : Case.Aux[SomeEntity, SomeEntity, Option[TheEvent] =
 *                 ...
 *     }
 * }}}
 *
 * Each defined [[shapeless.Cases.Case2]] is evaluated with `from` and `to` (in
 * that order).  Note that requisite collaborators __must__ be resolvable in the
 * `implicit` scope.  When they are needed, the [[shapeless.Cases.Case2]]
 * __must__ be an `implicit def`.  A common collaborator is an `implicit` scoped
 * environment and can be found in most ''InferPolyT'' definitions.
 */
final class InferDomainEvents[
	EntityT,
	InferPolyT <: Poly2,
	AllowableEventsT <: Coproduct
	] (
		private val from : EntityT,
		private val to : EntityT
	)
{
	/// Class Imports
	import mouse.any._


	/// Class Types
	sealed trait EventGenerator[PolyT <: Poly2, EventT]
	{
		def event : Option[AllowableEventsT]
	}


	object EventGenerator
		extends Cases
	{
		implicit def summoner[PolyT <: Poly2, EventT] (
			implicit
			@implicitNotFound (
				"could not resolve a handler in ${PolyT} having the " +
				"signature of " +
				"Case2.Aux[${PolyT}, ${EntityT}, ${EntityT}, Option[${EventT}]]."
				)
			handler : Case2.Aux[PolyT, EntityT, EntityT, Option[EventT]],

			@implicitNotFound (
				"${EventT} is not a member of ${AllowableEventsT}."
				)
			inject : coproduct.Inject[AllowableEventsT, EventT]
			)
			: EventGenerator[PolyT, EventT] =
			new EventGenerator[PolyT, EventT] {
				override val event = handler (from :: to :: HNil).map {
					inject (_)
					}
				}
	}


	object mkEvent
		extends Poly1
	{
		implicit def default[PolyT <: Poly2, EventT]
			: Case.Aux[
				EventGenerator[PolyT, EventT],
				Option[AllowableEventsT]
				] =
			at (_.event)
	}


	/**
	 * The toEvents method completes the inference process by creating a
	 * [[cats.data.Chain]] with zero or more ''AllowableEventsT'' produced by
	 * the ''InferPolyT'' polymorphic functor.
	 */
	def toEvents[
		AllAllowableEventsL <: HList,
		GeneratorL <: HList,
		ResultL <: HList
		] ()
		(
			implicit
			@unused
			toHList : coproduct.ToHList.Aux[
				AllowableEventsT,
				AllAllowableEventsL
				],

			liftAll : hlist.LiftAll.Aux[
				EventGenerator[InferPolyT, *],
				AllAllowableEventsL,
				GeneratorL
				],

			invoked : hlist.Mapper.Aux[mkEvent.type, GeneratorL, ResultL],
			toTraversable : hlist.ToTraversable.Aux[
				ResultL,
				List,
				Option[AllowableEventsT]
				]
			)
		: Chain[AllowableEventsT] =
		invoked (liftAll.instances) |>
		(toTraversable (_).flatMap (_.toList)) |>
		Chain.fromSeq
}

