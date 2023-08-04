package com.github.osxhacker.demo.chassis.adapter.kafka

import java.nio.charset.StandardCharsets

import cats.effect.Sync
import fs2.kafka.{
	Deserializer => Fs2Deserializer,
	Serializer => Fs2Serializer,
	_
	}

import io.circe.{
	Decoder,
	Encoder
	}

import io.circe.parser
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.{
	Serde,
	Serdes
	}


/**
 * The '''CirceAware''' type defines [[org.apache.kafka.common.serialization]]
 * support using [[io.circe]] [[io.circe.Decoder]]s and [[io.circe.Encoder]]s.
 */
trait CirceAware
{
	/// Class Imports
	import cats.syntax.either._


	/// Implicit Conversions
	implicit def deserializer[A] (
		implicit
		decoder : Decoder[A]
		)
		: KafkaDeserializer[A] =
		new KafkaDeserializer[A] {
			override def deserialize (topic : String, data : Array[Byte]) : A =
				Option (data).map {
					raw =>
						parser.decode[A] (
							new String (raw, StandardCharsets.UTF_8)
							)
							.valueOr {
								e =>
									throw new SerializationException (
										e.getMessage
										)
								}
					}
					.getOrElse (
						throw new KafkaException ("kafka data array is null")
						)
			}


	implicit def fs2Deserializer[F[_], A] (
		implicit
		kafkaDeserializer : KafkaDeserializer[A],
		sync : Sync[F]
		)
		: Fs2Deserializer[F, A] =
		Fs2Deserializer.delegate (kafkaDeserializer)


	implicit def fs2Serializer[F[_], A] (
		implicit
		kafkaSerializer : KafkaSerializer[A],
		sync : Sync[F]
		)
		: Fs2Serializer[F, A] =
		Fs2Serializer.delegate (kafkaSerializer)


	implicit def serializer[A] (implicit encoder : Encoder[A])
		: KafkaSerializer[A] =
		new KafkaSerializer[A] {
			override def serialize (topic : String, instance : A)
				: Array[Byte] =
				encoder (instance).noSpaces
					.getBytes (StandardCharsets.UTF_8)
			}


	implicit def serializerAndDeserializer[A] (
		implicit
		decoder : Decoder[A],
		encoder : Encoder[A]
		)
		: Serde[A] =
		Serdes.serdeFrom[A] (serializer[A], deserializer[A])
}

