package com.github.osxhacker.demo.chassis.adapter

import scala.jdk.CollectionConverters
import scala.language.postfixOps
import scala.util.Try

import cats.Eval
import cats.data.Kleisli
import cats.effect.{
	ExitCode,
	IO
	}

import kamon.Kamon
import org.typelevel.log4cats.LoggerFactory

import com.github.osxhacker.demo.chassis.domain.Specification
import com.github.osxhacker.demo.chassis.effect.Pointcut


/**
 * The '''KamonLifecycle''' type defines the contract for managing
 * initialization and quiescence of [[kamon.Kamon]] using the
 * [[https://www.artima.com/articles/scalas-stackable-trait-pattern Stackable Trait Pattern]].
 * This includes ensuring any [[java.lang.Thread]]s [[kamon.Kamon]] creates
 * which are non-daemons do not cause program termination to "hang."
 */
trait KamonLifecycle
	extends AbstractServer.LifecycleAdvice
{
	/// Class Imports
	import CollectionConverters._
	import KamonLifecycle._
	import ProgramArguments.OperatingMode
	import cats.syntax.applicative._
	import cats.syntax.flatMap._


	/// Instance Properties
	private lazy val abort = LoggerFactory.getLogger[IO]
		.debug ("halting service due to running Kamon threads") >>
		IO (Runtime.getRuntime halt ExitCode.Success.code)

	private val exitWhenKamonHasThreads : ExitCode => IO[ExitCode] =
		ec => IO.blocking {
			Thread.getAllStackTraces
				.keySet ()
				.asScala
				.filter (!IsDaemonThread && (IsKanelaThread || IsKamonThread))
				.map (_.getName)
			}
			.flatTap {
				threads =>
					IO.whenA (threads.nonEmpty) {
						val names = threads.mkString ("['", "', '", "']")

						LoggerFactory.getLogger[IO]
							.debug (s"Kamon user threads still running: $names")
						}
				}
			.map (_.nonEmpty && ec == ExitCode.Success)
			.flatMap (IO.whenA (_) (abort))
			.as (ec)

	private val initializer : Kleisli[IO, OperatingMode, Unit] =
		Kleisli liftF (IO blocking Kamon.init ())

	private val stopKamon = IO fromFuture (IO blocking Kamon.stop ())
	private val whenLeaving : ExitCode => IO[ExitCode] =
		stopKamon >> exitWhenKamonHasThreads (_)


	/**
	 * This apply method implementation uses two techniques in order to ensure
	 * proper [[kamon.Kamon]] management.  For initialization, the
	 * [[cats.data.Kleisli]] within the `super.apply` [[cats.effect.IO]] result
	 * is prefixed with the `initializer`. For orderly shutdown when there are
	 * no errors, the given '''pointcut''' `afterF` is used to perform the
	 * applicable steps.
	 */
	abstract override def apply (efa : Eval[Try[KleisliType]])
		(implicit pointcut : Pointcut[Try])
		: Eval[Try[KleisliType]] =
		pointcut.afterF (super.apply (efa) map (_ map (initializer >> _))) (
			leaving = _.flatMapF (whenLeaving).pure[Try]
			)
}


object KamonLifecycle
{
	/// Class Types
	/**
	 * The '''ThreadNamePredicate''' type defines the logic common to
	 * [[com.github.osxhacker.demo.chassis.domain.Specification]]s which
	 * identify [[java.lang.Thread]]s having a `substring` within their name
	 * (ignoring case).
	 */
	private sealed abstract class ThreadNamePredicate (
		private val substring : String
		)
		extends Specification[Thread]
	{
		final override def apply (candidate : Thread) : Boolean =
			Option (candidate.getName) exists (
				_.toLowerCase contains substring.toLowerCase
				)
	}


	private object IsDaemonThread
		extends Specification[Thread]
	{
		final override def apply (candidate : Thread) : Boolean =
			candidate.isDaemon
	}


	private object IsKamonThread
		extends ThreadNamePredicate ("kamon")


	private object IsKanelaThread
		extends ThreadNamePredicate ("kanela")
}

