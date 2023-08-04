package com.github.osxhacker.demo.chassis.adapter.kafka

import cats.Applicative
import cats.data.{
	EitherT,
	Kleisli,
	OptionT
	}

import com.github.osxhacker.demo.chassis.domain.Specification


/**
 * The '''EventFilter''' type defines the ability to filter Kafka API events
 * __before__ being transformed into domain events and interpreted.  This is
 * useful for ignoring events emitted by like microservices in the same
 * deployment [[com.github.osxhacker.demo.chassis.domain.event.Region]], for
 * example.
 *
 * Conceptually, '''EventFilter''' is an
 * [[https://en.wikipedia.org/wiki/Endomorphism Endomorphism]] in the context of
 * [[cats.data.Kleisli]].
 */
final case class EventFilter[F[_], EventT, EnvT] ()
	(
		implicit

		/// Needed for `fromOption` and `liftF`.
		private val applicative : Applicative[F]
	)
{
	/// Class Imports
	import mouse.any._
	import mouse.boolean._


	/**
	 * The apply method ensures that only ''EventT''s which are '''allow'''ed to
	 * continue do so.  All others are dropped at this point.
	 */
	def apply (allow : Specification[EventT])
		: EventProcessor[F, (EventT, EnvT), (EventT, EnvT)] =
		Kleisli {
			eventAndEnv =>
				allow (eventAndEnv._1).option (eventAndEnv) |>
				(OptionT.fromOption (_)) |>
				(EitherT.liftF (_))
				}
}

