package com.github.osxhacker.demo.chassis.adapter.kafka

import scala.annotation.implicitNotFound

import cats.{
	Monad,
	Show
	}

import cats.data.{
	EitherT,
	Kleisli,
	OptionT
	}

import org.typelevel.log4cats.{
	Logger,
	LoggerFactory
	}

import shapeless.{
	syntax => _,
	_
	}

import shapeless.ops.coproduct
import shapeless.ops.hlist


/**
 * The '''Ingress''' type is a model of the
 * [[https://en.wikipedia.org/wiki/Adapter_pattern ADAPTER]] pattern and is
 * responsible for transforming ''ApiEventT'' instances into one of the
 * supported ''DomainEventsT''.
 */
final class Ingress[
	ApiToDomainT <: Poly1,
	ApiEventT,
	AllApiEventsT <: Coproduct,
	TypeableApiEventsT <: HList,
	PairsT <: HList
	] (private val poly : ApiToDomainT)
	(
		implicit
		showApiEvent : Show[ApiEventT],
		typeables : coproduct.LiftAll.Aux[
			Typeable,
			AllApiEventsT,
			TypeableApiEventsT
			],

		zipConst : hlist.ZipConst.Aux[
			ApiEventT,
			TypeableApiEventsT,
			PairsT
			]
	)
{
	/// Class Imports
	import cats.syntax.flatMap._
	import cats.syntax.functor._
	import cats.syntax.show._
	import mouse.any._
	import mouse.option._


	def apply[
		F[_],
		DomainEventsT <: Coproduct,
		EnvT,
		OptionDomainEventsT <: HList
		] (interpret : Kleisli[F, (DomainEventsT, EnvT), Unit])
		(
			implicit
			monad : Monad[F],
			loggerFactory : LoggerFactory[F],

			@implicitNotFound (
				"unable to resolve Mapper for ${OptionDomainEventsT}, " +
				"make sure ${ApiToDomainT} has support for all types in " +
				"${AllApiEventsT}."
				)
			invokeTypeable : hlist.Mapper.Aux[
				poly.type,
				PairsT,
				OptionDomainEventsT
				],

			@implicitNotFound (
				"unable to resolve ToTraversable for ${OptionDomainEventsT}, " +
				"make sure each handler in ${ApiToDomainT} has the return type " +
				"of EitherT[Option, Throwable, ${DomainEventsT}]"
				)
			toTraversable : hlist.ToTraversable.Aux[
				OptionDomainEventsT,
				List,
				EitherT[Option, Throwable, DomainEventsT]
				]
		)
		: EventProcessor[F, (ApiEventT, EnvT), DomainEventsT] =
		Kleisli[EventProcessorContainer[F, *], (ApiEventT, EnvT), (DomainEventsT, EnvT)] {
			case (apiEvent, env) =>
				typeables.instances
					.zipConst (apiEvent)
					.map (poly)
					.toList[EitherT[Option, Throwable, DomainEventsT]]
					.map (_.value)
					.find (_.isDefined)
					.flatten
					.cata (
						_.fold (
							invalidEvent (apiEvent),
							ev => EitherT.liftF (OptionT.pure[F] (ev -> env))
							),

						unsupportedEvent (apiEvent)
						)
			}
			.andThen {
				Kleisli[EventProcessorContainer[F, *], (DomainEventsT, EnvT), DomainEventsT] {
					case (domainEvent, env) =>
						EitherT.liftF (
							OptionT.liftF (
								interpret (domainEvent -> env).as (domainEvent)
								)
							)
					}
				}


	private def invalidEvent[F[_], ResultT] (apiEvent : ApiEventT)
		(problem : Throwable)
		(
			implicit
			monad : Monad[F],
			loggerFactory : LoggerFactory[F]
		)
		: EventProcessorContainer[F, ResultT] =
		discard (apiEvent) {
			_.error (problem) ("unable to transform event")
			}


	private def unsupportedEvent[F[_], ResultT] (apiEvent : ApiEventT)
		(
			implicit
			monad : Monad[F],
			loggerFactory : LoggerFactory[F]
		)
		: EventProcessorContainer[F, ResultT] =
		discard (apiEvent) {
			_.info ("ignoring unsupported event")
			}


	private def discard[F[_], A] (apiEvent : ApiEventT)
		(statement : Logger[F] => F[Unit])
		(
			implicit
			monad : Monad[F],
			loggerFactory : LoggerFactory[F]
		)
		: EventProcessorContainer[F, A] =
		EitherT.liftF (
			OptionT (
				loggerFactory.create
					.flatMap {
						_.addContext (Map ("event" -> apiEvent.show)) |>
						statement
					}
					.as (None)
			)
		)
}


object Ingress
{
	/// Class Types
	final class PartiallyAppliedApply[ApiEventT, AllApiEventsT <: Coproduct] ()
	{
		def apply[
			ApiToDomainT <: Poly1,
			TypeableApiEventsT <: HList,
			PairsT <: HList
			] (poly : ApiToDomainT)
			(
				implicit
				showApiEvent : Show[ApiEventT],
				typeables : coproduct.LiftAll.Aux[
					Typeable,
					AllApiEventsT,
					TypeableApiEventsT
					],

				zipConst : hlist.ZipConst.Aux[
					ApiEventT,
					TypeableApiEventsT,
					PairsT
					]
			)
		: Ingress[
				ApiToDomainT,
				ApiEventT,
				AllApiEventsT,
				TypeableApiEventsT,
				PairsT
				] =
			new Ingress[
				ApiToDomainT,
				ApiEventT,
				AllApiEventsT,
				TypeableApiEventsT,
				PairsT
			] (poly)
	}


	/**
	 * The apply method employs the "partially applied" idiom to facilitate
	 * creating an
	 * [[com.github.osxhacker.demo.chassis.adapter.kafka.Ingress]] by only
	 * requiring collaborators to provide ''ApiEventT'' plus the
	 * ''AllApiEventsT '', allowing the compiler to derive the rest.
	 */
	@inline
	def apply[ApiEventT, AllApiEventsT <: Coproduct]
		: PartiallyAppliedApply[ApiEventT, AllApiEventsT] =
		new PartiallyAppliedApply[ApiEventT, AllApiEventsT] ()
}

