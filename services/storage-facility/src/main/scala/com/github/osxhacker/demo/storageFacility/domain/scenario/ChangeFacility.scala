package com.github.osxhacker.demo.storageFacility.domain.scenario

import scala.language.postfixOps
import scala.reflect.ClassTag

import cats.MonadThrow
import cats.data.{
	Kleisli,
	ValidatedNec
	}

import eu.timepit.refined
import monocle.{
	syntax => _,
	_
	}

import shapeless.{
	syntax => _,
	_
	}

import squants.space.Volume

import com.github.osxhacker.demo.chassis.domain.ErrorOr
import com.github.osxhacker.demo.chassis.domain.entity.{
	Identifier,
	Version
	}

import com.github.osxhacker.demo.chassis.domain.error._
import com.github.osxhacker.demo.chassis
import com.github.osxhacker.demo.chassis.domain.event.{
	EventLog,
	EventPolicy,
	Region
	}

import com.github.osxhacker.demo.chassis.domain.repository.UpdateIntent
import com.github.osxhacker.demo.chassis.effect.Pointcut
import com.github.osxhacker.demo.chassis.monitoring.metrics.UseCaseScenario
import com.github.osxhacker.demo.storageFacility.domain._
import com.github.osxhacker.demo.storageFacility.domain.event.{
	AllStorageFacilityEvents,
	StorageFacilityChangeEvents
	}

import ChangeFacility.FactoryParamsType


/**
 * The '''ChangeFacility''' type defines the Use-Case scenario responsible for
 * validating the information known to produce an
 * [[com.github.osxhacker.demo.chassis.domain.repository.Intent]] applicable for
 * modifying an existing
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]].
 */
sealed abstract class ChangeFacility[F[_], SourceT <: AnyRef] (
	private val id : AdaptOptics.KleisliType[SourceT, Identifier[StorageFacility]],
	private val version : AdaptOptics.KleisliType[SourceT, Version],
	private val status : AdaptOptics.KleisliType[SourceT, StorageFacilityStatus],
	private val available : AdaptOptics.KleisliType[SourceT, Volume],
	private val capacity : AdaptOptics.KleisliType[SourceT, Volume]
	)
	(
		private val factory : Kleisli[
			ErrorOr,
			FactoryParamsType[SourceT],
			StorageFacility
			]
	)
	(
		implicit

		/// Needed for `flatMap` and `raiseError`.
		private val monadThrow : MonadThrow[F],

		/// Needed for '''ValidationError'''.
		private val classTag : ClassTag[SourceT],

		/// Needed for `Aspect`.
		private val pointcut : Pointcut[F],

		/// Needed for `broadcast`.
		private val policy : EventPolicy[
			F,
			ScopedEnvironment[F],
			AllStorageFacilityEvents
			]
	)
{
	/// Class Imports
	import cats.syntax.all._
	import chassis.syntax._
	import mouse.boolean._


	/// Instance Properties
	protected val allowChange : CanModify
	private val save = SaveFacility[F] ()
	private val unit : Unit = {}


	override def toString () : String = "scenario: change facility"


	def apply (existing : StorageFacility, source : SourceT)
		(implicit env : ScopedEnvironment[F])
		: F[StorageFacility] =
		(canChange (existing) flatMap (change (_, source))).broadcast ()
			.measure[
				UseCaseScenario[
					F,
					ChangeFacility[F, SourceT],
					StorageFacility
					]
				] ()


	/**
	 * The enableMultiRegion method configures a new '''ChangeFacility'''
	 * instance to allow invocations to proceed on more than just for the
	 * [[com.github.osxhacker.demo.chassis.domain.event.Region]] a service is
	 * deployed.
	 */
	def enableMultiRegion () : ChangeFacility[F, SourceT] =
		new ChangeFacility[F, SourceT] (
			id,
			version,
			status,
			available,
			capacity
			) (factory) {
			override val allowChange = CanModify.AlwaysAllow
			}


	private def canChange (existing : StorageFacility)
		(implicit env: ScopedEnvironment[F])
		: EventLog[F, StorageFacility, AllStorageFacilityEvents] =
		EventLog.liftF (allowChange (existing))


	private def change (existing : StorageFacility, source : SourceT)
		(implicit env : ScopedEnvironment[F])
		: EventLog[F, StorageFacility, AllStorageFacilityEvents] =
		(validate (existing, source) >>= factory.run)
			.map (UpdateIntent (_).filter (_.differsFrom (existing)))
			.liftTo[EventLog[F, *, AllStorageFacilityEvents]]
			.flatMap {
				intent =>
					EventLog.liftF (save (intent))
				}
			.mapBoth {
				case (events, Some (saved)) =>
					(
					events |+| existing.infer[StorageFacilityChangeEvents] (
						saved
						)
						.toEvents ()
						.map (_.embed[AllStorageFacilityEvents]),

					saved
					)

				case (events, None) =>
					events -> existing
				}


	private def checkForLogicErrors (
		existing : StorageFacility,
		source : SourceT
		)
		: ErrorOr[Version] =
		id (source).andThen {
			submitted =>
				(submitted === existing.id).validatedNec (
					s"${toString ()} - storage facility id's do not match",
					unit
					)
			}
			.productR (version (source))
			.leftMap (errs => LogicError (errs.head))
			.toEither


	private def checkFutureVersion (existing : StorageFacility, source : SourceT)
		: ValidatedNec[String, Unit] =
		version (source) andThen {
			submitted =>
				(submitted <= existing.version).validatedNec (
					s"${toString ()} - 'future' storage facility version detected: ${submitted.show}",
					unit
					)
			}


	private def checkStaleVersion (existing : StorageFacility)
		(submitted : Version)
		: ErrorOr[Unit] =
		(submitted >= existing.version).either (
			StaleObjectError[StorageFacility] (
				existing.id,
				submitted,
				latest = existing.some
				),

			unit
			)


	private def checkStatus (existing : StorageFacility, source : SourceT)
		: ValidatedNec[String, Unit] =
		status (source) andThen {
			desired =>
				val current = existing.status

				(current === desired || current.canBecome (desired))
					.validatedNec (
						s"${toString ()} - cannot transition from '$current' to '$desired'",
						unit
						)
			}


	private def validate (existing : StorageFacility, source : SourceT)
		(implicit env : ScopedEnvironment[F])
		: ErrorOr[FactoryParamsType[SourceT]] =
		(
			checkForLogicErrors (existing, source) >>=
			checkStaleVersion (existing)
		) >>
		(
			ValidateFacilityOwnership (existing, env.tenant) *>
			checkFutureVersion (existing, source) *>
			checkStatus (existing, source) *>
			ValidateStorageVolume (source, available, capacity)
		).bimap (
			ValidationError[SourceT] (_),
			_ =>
				source ::
				/// When changing a '''StorageFacility''', the `existing`
				/// '''Region''' is retained if present.
				existing.primary.getOrElse (env.region) ::
				existing.owner ::
				HNil
			)
			.toEither
}


object ChangeFacility
{
	/// Class Imports
	import StorageFacility.primary
	import cats.syntax.eq._
	import refined.api.Refined


	/// Class Types
	private final type FactoryParamsType[A] = A :: Region :: Company :: HNil


	final class PartiallyApplied[F[_]] ()
	{
		def apply[SourceT <: AnyRef, StatusT <: AnyRef, IdT, VolumeT, P1, P2, P3] (
			id : Getter[SourceT, IdT],
			version : Getter[SourceT, Refined[Int, P1]],
			status : Getter[SourceT, StatusT],
			available : Getter[SourceT, Refined[VolumeT, P2]],
			capacity : Getter[SourceT, Refined[VolumeT, P3]]
			)
			(factory : Kleisli[ErrorOr, FactoryParamsType[SourceT], StorageFacility])
			(
				implicit
				monadThrow : MonadThrow[F],
				classTag : ClassTag[SourceT],
				parser : Identifier.Parser[StorageFacility, IdT],
				numeric : Numeric[VolumeT],
				pointcut : Pointcut[F],
				policy : EventPolicy[
					F,
					ScopedEnvironment[F],
					AllStorageFacilityEvents
					]
			)
			: ChangeFacility[F, SourceT] =
			new ChangeFacility[F, SourceT] (
				id = AdaptOptics.id (id),
				version = AdaptOptics.version (version),
				status = AdaptOptics.status (status),
				available = AdaptOptics.availability (available),
				capacity = AdaptOptics.capacity (capacity)
				) (factory) {
				override val allowChange = CanModify.PrimaryRegion
				}
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

