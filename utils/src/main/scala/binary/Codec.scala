package binary

import binary.Codec.Decoder.Decoded
import cats.data.ValidatedNel
import fs2.Chunk

object Codec {
  trait Decoder[T] { self =>
    def decode(bytes: Chunk[Byte]): Decoded[T]
  }

  object Decoder {
    type Decoded[V] = ValidatedNel[DecodedFailure, V]

    sealed abstract class DecodedFailure(info: String) extends Product with Serializable
    case object WrongOpcode                            extends DecodedFailure("opcode number is wrong")
    case object WrongMode                              extends DecodedFailure("mode is not netascii")

    def apply[T: Decoder]: Decoder[T] = implicitly[Decoder[T]]
    def instance[T](fn: Chunk[Byte] => Decoded[T]): Decoder[T] =
      (chunk: Chunk[Byte]) => fn(chunk)
  }

  trait Encoder[T] { self =>
    def encode(t: T): Chunk[Byte]
    def contramap[A](f: A => T): Encoder[A] = (t: A) => self.encode(f(t))
  }

  object Encoder {
    def apply[T: Encoder]: Encoder[T]                 = implicitly[Encoder[T]]
    def instance[T](fn: T => Chunk[Byte]): Encoder[T] = (t: T) => fn(t)
  }

  trait Codec[T] extends Decoder[T] with Encoder[T]
  object Codec {
    def from[T](decoder: Decoder[T], encoder: Encoder[T]): Codec[T] =
      new Codec[T] {
        def encode(t: T): Chunk[Byte]              = encoder.encode(t)
        def decode(bytes: Chunk[Byte]): Decoded[T] = decoder.decode(bytes)
      }
  }
}
