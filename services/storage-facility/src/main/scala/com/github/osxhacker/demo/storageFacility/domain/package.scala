package com.github.osxhacker.demo.storageFacility

import eu.timepit.refined.api.{
	Refined,
	RefinedType
	}

import io.scalaland.chimney.TransformerF
import squants.space.{
	CubicMeters,
	Volume
	}

import com.github.osxhacker.demo.chassis.domain.{
	ChimneyErrors,
	ChimneyTransformers
	}


/**
 * The '''domain''' `package` contains types which define the Domain Object
 * Model specific to storage facilities.
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
		implicit def fromVolumeTransformerF[FTP] (
			implicit rt : RefinedType.AuxT[FTP, BigDecimal]
			)
			: TransformerF[ChimneyErrors, Volume, FTP] =
			new TransformerF[ChimneyErrors, Volume, FTP] {
				override def transform (src : Volume) : ChimneyErrors[FTP] =
					rt.refine (src.value)
						.toValidatedNec
				}


		implicit def toVolumeTransformerF[T, P] (implicit numeric : Numeric[T])
			: TransformerF[ChimneyErrors, Refined[T, P], Volume] =
			new TransformerF[ChimneyErrors, Refined[T, P], Volume] {
				override def transform (src : Refined[T, P])
					: ChimneyErrors[Volume] =
					CubicMeters (src.value).validNec
				}
	}
}

