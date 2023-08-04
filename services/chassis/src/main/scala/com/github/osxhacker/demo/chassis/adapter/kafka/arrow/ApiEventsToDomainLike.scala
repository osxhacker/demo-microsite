package com.github.osxhacker.demo.chassis.adapter.kafka.arrow

import scala.reflect.ClassTag

import cats.data.EitherT
import shapeless.{
	syntax => _,
	_
	}

import com.github.osxhacker.demo.chassis.domain.ChimneyErrors
import com.github.osxhacker.demo.chassis.domain.error.ValidationError


/**
 * The '''ApiEventsToDomainLike''' type defines reusable types and behaviour
 * related to implementing transformations from API event types to their
 * corresponding domain representations.
 */
trait ApiEventsToDomainLike[ApiBaseT]
	extends Poly1
{
	/// Class Imports
	import cats.syntax.bifunctor._


	/// Class Types
	final type ResultType[ApiT, DomainT] = Case.Aux[
		(Typeable[ApiT], ApiBaseT),
		EitherT[Option, Throwable, DomainT]
		]


	/**
	 * The ignore method is provided so that concrete implementations can
	 * explicitly declare a specific ''ApiT'' to be not supported.
	 */
	protected def ignore[ApiT, DomainT] () : ResultType[ApiT, DomainT] =
		new CaseBuilder[(Typeable[ApiT], ApiBaseT)]
			.apply[EitherT[Option, Throwable, DomainT]] {
				_ => EitherT.liftF (None)
			}


	/**
	 * The transform method is a helper which ensures the logic defined by
	 * '''fa''' is provided in a manner which can become a
	 * [[com.github.osxhacker.demo.chassis.adapter.kafka.arrow.ApiEventsToDomainLike.ResultType]].
	 */
	protected def transform[ApiT, DomainT] (fa : ApiT => ChimneyErrors[DomainT])
		(implicit classTag : ClassTag[ApiT])
		: ResultType[ApiT, DomainT] =
		new CaseBuilder[(Typeable[ApiT], ApiBaseT)]
			.apply[EitherT[Option, Throwable, DomainT]] {
				params =>
					EitherT (
						params._1
							.cast (params._2)
							.map {
								fa (_).toEither
									.leftMap (ValidationError[ApiT] (_))
							}
					)
				}
}

