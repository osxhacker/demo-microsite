package $package$

import cats.data.ValidatedNec
import eu.timepit.refined.api.{
	Refined,
	RefinedType
	}

import io.scalaland.chimney.TransformerF

import com.github.osxhacker.demo.chassis.domain.ChimneyTransformers


/**
 * The '''domain''' `package` contains types which define the Domain Object
 * Model specific to $name$.
 */
package object domain
{
	/// Class Imports
	import cats.syntax.either._
	import cats.syntax.validated._


	/// Class Types
	/**
	 * The '''transformers''' `object` contains Chimney
	 * [[io.scalaland.chimney.TransformerF]] definitions related to defining
	 * domain-specific instance transformations.
	 */
	object transformers
		extends ChimneyTransformers
	{
		/// Implicit Conversions
	}
}

