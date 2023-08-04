package com.github.osxhacker.demo.company

import com.github.osxhacker.demo.chassis.domain.ChimneyTransformers


/**
 * The '''domain''' `package` contains types which define the Domain Object
 * Model specific to Company.
 */
package object domain
{
	/// Class Types
	/**
	 * The '''transformers''' `object` contains Chimney
	 * [[io.scalaland.chimney.TransformerF]] definitions related to defining
	 * domain-specific instance transformations.
	 */
	object transformers
		extends ChimneyTransformers
}

