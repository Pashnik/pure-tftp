package binary

import binary.Codec.Decoder.{Decoded, DynamicFailure}
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
              .leftMap(e => DynamicFailure(e.getMessage))
        }
  }

  object Decoder {
    type Decoded[V] = Validated[Failure, V]

    sealed abstract class Failure(info: String) extends Product with Serializable
    case object WrongOpcode                     extends Failure("opcode number is wrong")
    case object WrongMode                       extends Failure("mode is not netascii")
    case object DataOverspend                   extends Failure("there is more then 512 bytes in a chunk")
    case class DynamicFailure(info: String)     extends Failure(info)

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
