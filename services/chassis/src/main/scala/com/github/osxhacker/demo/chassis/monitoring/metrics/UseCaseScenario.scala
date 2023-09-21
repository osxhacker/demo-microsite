package com.github.osxhacker.demo.chassis.monitoring.metrics

import scala.reflect.ClassTag

import cats.ApplicativeThrow

import com.github.osxhacker.demo.chassis.effect.DefaultAdvice


/**
 * The '''UseCaseScenario''' type defines the standard
 * [[com.github.osxhacker.demo.chassis.monitoring.metrics.MetricsAdvice]]
 * intended for use in measuring Use-Case scenarios.
 */
final class UseCaseScenario[F[_], ScenarioT, ResultT] ()
	(
		implicit

		/// Needed for '''DefaultAdvice'''.
		override protected val applicativeThrow : ApplicativeThrow[F],

		private val classTag : ClassTag[ScenarioT],
	)
	extends DefaultAdvice[F, ResultT] ()
		with MetricsAdvice[F, ResultT]
		with InvocationCounters[F, ResultT]
		with ScopeInternalOperations[F, ResultT]
{
	/// Instance Properties
	override val component = "use-case"
	override val group = subgroup (
		super.group,
		"scenario",
		classTag.runtimeClass.getSimpleName
		)

	override val operation = classTag.runtimeClass.getSimpleName
}


object UseCaseScenario
{
	/// Implicit Conversions
	implicit def summon[F[_], ScenarioT, ResultT] (
		implicit
		applicativeThrow: ApplicativeThrow[F],
		classTag : ClassTag[ScenarioT]
		)
		: UseCaseScenario[F, ScenarioT, ResultT] =
		new UseCaseScenario[F, ScenarioT, ResultT] ()
}

