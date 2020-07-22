package binary

import Tftp.{ErrorCode, Undefined}
import binary.Codec.Decoder.Decoded
import cats.data.Validated
import fs2.Chunk
import scala.util.Try

object Codec {
  trait Decoder[T] { self =>
    def decode(bytes: Chunk[Byte]): Decoded[T]

    def emapTry[O](f: T => Try[O]): Decoder[O] =
      (bytes: Chunk[Byte]) =>
        self
          .decode(bytes)
          .andThen { t =>
            Validated
              .fromTry(f(t))
              .leftMap(e => Undefined(s"Error during decoding: ${e.getMessage}"))
        }
  }

  object Decoder {
    type Decoded[V] = Validated[ErrorCode, V]

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

  object syntax {
    implicit class EncoderOps[T](val t: T) {
      def encoded(implicit E: Encoder[T]) = E.encode(t)
    }

    implicit class ChunkOps(private val underlying: Chunk[Byte]) {
      def as[T: Decoder] = Decoder[T].decode(underlying)
      def extractOpcode(): Opcode =
        Buffer
          .fromChunk(underlying)
          .iterableOnce
          .number[Opcode]()
    }
  }
}
