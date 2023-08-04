package com.github.osxhacker.demo


/**
 * The '''chassis''' `package` contains types which assist in the construction
 * of micro-services.
 */
package object chassis
{
	/// Class Types
	object syntax
		extends domain.event.EventSyntax
			with effect.AspectSyntax
			with monitoring.metrics.MetricsSyntax
}

