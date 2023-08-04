package $package$.adapter

import cats.effect.{
	Async,
	Ref
	}

import org.typelevel.log4cats.LoggerFactory

import com.github.osxhacker.demo.chassis.effect.ReadersWriterResource
import $package$.domain.GlobalEnvironment


/**
 * The '''ProvisionEnvironment''' `object` provides the service with the ability
 * to manufacture
 * [[$package$.domain.GlobalEnvironment]]s based
 * on the contents of
 * [[$package$.adapter.RuntimeSettings]]
 * instances.
 */
object ProvisionEnvironment
{
	/// Class Imports
	import cats.syntax.flatMap._


	def apply[F[_], A] (settings : RuntimeSettings)
		(f : ReadersWriterResource[F, GlobalEnvironment[F]] => F[A])
		(
			implicit
			make : Ref.Make[F],
			async : Async[F],
			loggerFactory : LoggerFactory[F]
		)
		: F[A] =
	{
		val global = GlobalEnvironment[F] ()

		ReadersWriterResource.from (global) >>= f
	}


	private def processors () : Int = Runtime.getRuntime.availableProcessors ()
}

