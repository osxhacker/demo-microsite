package com.github.osxhacker.demo.chassis.adapter.rest

import cats.{
	Applicative,
	MonadThrow
	}

import org.scalatest.AsyncTestSuite
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.noop.NoOpFactory
import sttp.client3.testing.SttpBackendStub
import sttp.monad.{
	MonadError => SttpMonadError
	}

import sttp.tapir.server.stub.TapirStubInterpreter


/**
 * The '''TapirClientSupport''' type provides functionality for creating and
 * using [[sttp.client3]] "stub" interpreters in resource unit tests.
 */
trait TapirClientSupport
{
	/// Self Type Constraints
	this : AsyncTestSuite =>


	/// Class Imports
	import TapirClientSupport.SttpMonadErrorShim


	/**
	 * The stubInterpreter method creates a ready-to-use
	 * [[sttp.tapir.server.stub.TapirStubInterpreter]] which executes within the
	 * ''F'' container.
	 */
	protected def stubInterpreter[F[_], CapabilitiesT] ()
		(implicit monadThrow : MonadThrow[F])
		: TapirStubInterpreter[F, CapabilitiesT, Unit] =
		TapirStubInterpreter (
			SttpBackendStub[F, CapabilitiesT] (responseMonadFor[F] ())
			)


	private def responseMonadFor[F[_]] ()
		(implicit monadThrow : MonadThrow[F])
		: SttpMonadError[F] =
		new SttpMonadErrorShim[F] ()


	/// Implicit Conversions
	implicit protected def noopLoggerFactory[F[_]] (
		implicit applicative : Applicative[F]
		)
		: LoggerFactory[F] =
		NoOpFactory[F]
}


object TapirClientSupport
{
	/// Class Types
	/**
	 * The '''SttpMonadErrorShim''' type adapts arbitrary [[cats.MonadError]]
	 * capable ''F'' types into [[sttp.monad.MonadError]]s.
	 */
	final class SttpMonadErrorShim[F[_]] ()
		(implicit private val catsMonadError : MonadThrow[F])
		extends SttpMonadError[F]
	{
		override def unit[T] (t : T): F[T] = catsMonadError.pure (t)


		override def map[T, T2] (fa : F[T])
			(f : T => T2)
			: F[T2] =
			catsMonadError.map (fa) (f)


		override def flatMap[T, T2] (fa : F[T])
			(f : T => F[T2])
			: F[T2] =
			catsMonadError.flatMap (fa) (f)


		override def error[T] (t : Throwable) : F[T] =
			catsMonadError.raiseError (t)


		override protected def handleWrappedError[T] (ft : F[T])
			(f : PartialFunction[Throwable, F[T]])
			: F[T] =
			catsMonadError.recoverWith (ft) (f)


		override def ensure[T] (ft : F[T], e : => F[Unit]) : F[T] =
			catsMonadError.redeemWith (ft) (
				err => flatMap (e) (_ => catsMonadError.raiseError[T] (err)),
				t => map (e) (_ => t)
			)
	}
}
