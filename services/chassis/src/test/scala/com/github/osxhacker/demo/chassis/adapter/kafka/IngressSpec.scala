package com.github.osxhacker.demo.chassis.adapter.kafka

import cats.Show
import cats.data.{
	EitherT,
	Kleisli
	}

import org.scalatest.diagrams.Diagrams
import org.scalatest.wordspec.AnyWordSpec
import org.typelevel.log4cats.noop.NoOpFactory
import shapeless.{
	syntax => _,
	_
	}

import shapeless.ops.coproduct

import com.github.osxhacker.demo.chassis.domain.{
	ErrorOr,
	Specification
	}


/**
 * The '''IngressSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.chassis.adapter.ProgramArguments]] for fitness
 * of purpose and serves as an exemplar of its use.
 */
final class IngressSpec ()
	extends AnyWordSpec
		with Diagrams
{
	/// Class Types
	type SampleApiTypes = String :+: Int :+: Double :+: CNil


	type SampleDomainTypes = StringChanged :+: IntChanged :+: CNil


	final case class EmptyEnvironment ()


	final case class IntChanged (value : Int)


	final case class StringChanged (value : String)


	object MakeDomainInstance
		extends Poly1
	{
		/// Example of an api type which is not used/supported.
		implicit val caseDouble : Case.Aux[
			(Typeable[Double], Any),
			EitherT[Option, Throwable, SampleDomainTypes]
			] =
			at (_ => EitherT.liftF (None))

		implicit val caseInt : Case.Aux[
			(Typeable[Int], Any),
			EitherT[Option, Throwable, SampleDomainTypes]
			] =
			at {
				case (typeable, value) =>
					EitherT.liftF (
						typeable.cast (value)
						.map {
							i =>
								coproduct.Inject[SampleDomainTypes, IntChanged]
									.apply (IntChanged (i))
							}
						)
				}

		implicit val caseString : Case.Aux[
			(Typeable[String], Any),
			EitherT[Option, Throwable, SampleDomainTypes]
			] =
			at {
				case (typeable, value) =>
					EitherT.liftF (
						typeable.cast (value)
						.map {
							s =>
								coproduct.Inject[SampleDomainTypes, StringChanged]
									.apply (StringChanged (s))
							}
						)
				}
	}


	/// Instance Properties
	implicit val loggerFactory = NoOpFactory[ErrorOr]
	implicit val showAny : Show[Any] = Show.fromToString


	"The Ingress adapter" must {
		"be able to compose a domain-based interpreter" in {
			val fromApi : Any = 42
			val ingress = Ingress[Any, SampleApiTypes] (MakeDomainInstance)
			val interpreter = ingress (
				Kleisli[ErrorOr, (SampleDomainTypes, EmptyEnvironment), Unit] {
					case (instance, _) =>
						assert (instance.select[IntChanged].isDefined)
						assert (instance.select[StringChanged].isEmpty)
						assert (
							instance.select[IntChanged] === Option (IntChanged (42))
							)

						Right ({})
					}
				)

			val result = interpreter (fromApi -> EmptyEnvironment ()).value
				.value

			assert (result.isRight)
			assert (result.exists (_.isDefined))
			assert (result.exists (_.exists (_.isRight)))
			}

		"require all api events are accounted for during compilation" in {
			assertDoesNotCompile (
				"""
				type HasUnsupportedTypes = Boolean :+: SampleApiTypes


				val ingress = Ingress[Any, HasUnsupportedTypes] (MakeDomainInstance)

				ingress (
					Kleisli[ErrorOr, (SampleDomainTypes, EmptyEnvironment), Unit] {
						_ =>
							Right ({})
						}
					)
				"""
				)
			}

		"support filtering 'raw' api events" in {
			val ingress = Ingress[Any, SampleApiTypes](MakeDomainInstance)
			val isNotEmpty = Specification[String] (_.nonEmpty)
			val filter = EventFilter[ErrorOr, String, EmptyEnvironment] ()
			val interpreter = filter (isNotEmpty) andThen ingress (
				Kleisli[ErrorOr, (SampleDomainTypes, EmptyEnvironment), Unit] {
					case (instance, _) =>
						assert (instance.select[IntChanged].isEmpty)
						assert (instance.select[StringChanged].isDefined)

						Right ({})
					}
				)

			val allowed = interpreter ("hello" -> EmptyEnvironment ())
				.value
				.value

			val disallowed = interpreter ("" -> EmptyEnvironment ()).value
				.value

			assert (allowed.exists (_.isDefined))
			assert (allowed.exists (_.exists (_.isRight)))

			assert (disallowed.exists (_.isEmpty))
			}
		}
}

