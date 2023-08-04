package com.github.osxhacker.demo.chassis.effect


/**
 * The '''Aspect''' `object` provides the ability to bring together
 * [[com.github.osxhacker.demo.chassis.effect.Advice]] with specific
 * [[com.github.osxhacker.demo.chassis.effect.Pointcut]]s in order to define
 * both `static` and `percall`
 * [[https://en.wikipedia.org/wiki/Aspect-oriented_programming Aspect Oriented Programming]]
 * logic.
 *
 * Static [[com.github.osxhacker.demo.chassis.effect.Advice]] can be
 * instantiated once and do not support context-specific [[kamon.tag.TagSet]]s
 * (though they can use constant [[kamon.tag.TagSet]]s if desired).
 *
 * Per-call [[com.github.osxhacker.demo.chassis.effect.Advice]] are instantiated
 * with __each__ use and can have a context-specific [[kamon.tag.TagSet]].  They
 * incur more overhead but are necessary for the aforementioned requirement.
 */
object Aspect
{
	/// Class Types
	final class PartiallyAppliedAspect[F[_], AdviceT <: Advice[F, _]] ()
	{
		/**
		 * The percall method creates a
		 * [[com.github.osxhacker.demo.chassis.effect.PercallAspect]] which
		 * supports creating and applying an ''AdviceT'' on __each__ invocation.
		 */
		def percall[ContextT] (factory : ContextT => AdviceT)
			(implicit pointcut : Pointcut[F])
			: PercallAspect[F, AdviceT, ContextT] =
			PercallAspect[F, AdviceT, ContextT] (factory)


		/**
		 * This version of the static method creates a
		 * [[com.github.osxhacker.demo.chassis.effect.StaticAspect]] which is
		 * bound to an
		 * [[com.github.osxhacker.demo.chassis.effect.Advice]] able to be
		 * created with __no__ execution context.
		 */
		def static[ResultT] ()
			(implicit staticAspect : StaticAspect.Aux[F, AdviceT, ResultT])
			: StaticAspect.Aux[F, AdviceT, ResultT] =
			staticAspect


		/**
		 * This version of the static method creates a
		 * [[com.github.osxhacker.demo.chassis.effect.StaticAspect]] which is
		 * bound to a given
		 * [[com.github.osxhacker.demo.chassis.effect.Advice]] created __once__
		 * when static is invoked.
		 */
		def static[ResultT] (advice : => AdviceT)
			(
				implicit
				ev : AdviceT <:< Advice[F, ResultT],
				pointcut : Pointcut[F]
			)
			: StaticAspect.Aux[F, AdviceT, ResultT] =
			StaticAspect (advice)
	}


	/**
	 * The apply method is provided to support functional-style creation and
	 * employs the "partially applied" idiom, thus only requiring collaborators
	 * to provide ''F'' and ''AdviceT'', allowing the compiler to deduce the
	 * remaining type parameters.
	 */
	@inline
	def apply[F[_], AdviceT <: Advice[F, _]] : PartiallyAppliedAspect[F, AdviceT] =
		new PartiallyAppliedAspect[F, AdviceT] ()
}

