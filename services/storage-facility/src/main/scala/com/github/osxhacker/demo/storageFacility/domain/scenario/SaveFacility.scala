package com.github.osxhacker.demo.storageFacility.domain.scenario

import scala.language.postfixOps

import cats.Monad
import org.typelevel.log4cats.Logger

import com.github.osxhacker.demo.chassis
import com.github.osxhacker.demo.chassis.domain.repository.Intent
import com.github.osxhacker.demo.chassis.effect.Pointcut
import com.github.osxhacker.demo.chassis.monitoring.metrics.UseCaseScenario
import com.github.osxhacker.demo.storageFacility.domain.{
	ScopedEnvironment,
	StorageFacility
	}


/**
 * The '''SaveFacility''' type defines the Use-Case scenario responsible for
 * attempting to persist a
 * [[com.github.osxhacker.demo.storageFacility.domain.StorageFacility]] based on
 * an [[com.github.osxhacker.demo.chassis.domain.repository.Intent]] given.
 */
final case class SaveFacility[F[_]] ()
	(
		implicit

		/// Needed for `flatMap`.
		private val monad : Monad[F],

		/// Needed for `measure`.
		private val pointcut : Pointcut[F]
	)
{
	/// Class Imports
	import cats.syntax.all._
	import chassis.syntax._


	override def toString () : String = "scenario: save facility"


	def apply (intent : Intent[StorageFacility])
		(implicit env : ScopedEnvironment[F])
		: F[Option[StorageFacility]] =
		save (intent).measure[
			UseCaseScenario[
				F,
				SaveFacility[F],
				Option[StorageFacility]
				]
			] ()


	private def entering (intent : Intent[StorageFacility])
		(implicit logger : Logger[F])
		: F[Unit] =
		intent.fold (
			monad.unit,
			sf => logger.debug (s"${toString ()} - saving storage facility: " + sf.id.show)
			)


	private def leaving (result : Option[StorageFacility])
		(implicit logger : Logger[F])
		: F[Unit] =
		result.fold (monad.unit) {
			instance =>
				logger.debug (s"${toString ()} - saved storage facility: " + instance.id.show)
			}


	private def save (intent : Intent[StorageFacility])
		(implicit env : ScopedEnvironment[F])
		: F[Option[StorageFacility]] =
		for {
			implicit0 (logger : Logger[F]) <- env.loggingFactory.create

			_ <- entering (intent)
			result <- env.storageFacilities.save (intent)
			_ <- leaving (result)
			} yield result
}

