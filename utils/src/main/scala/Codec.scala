import Codec.Decoder.DecodedFailure
import fs2.Chunk

object Codec {
  trait Decoder[T] {
    type Decoded = Either[T, DecodedFailure]

    def decode(bytes: Chunk[Byte]): Decoded
  }
  object Decoder {
    sealed abstract class DecodedFailure(info: String) extends Product with Serializable
    case object A                                      extends DecodedFailure("A")
    case object B                                      extends DecodedFailure("B")

    def apply[T: Decoder]: Decoder[T] = implicitly[Decoder[T]]
    def instance[T](fn: Chunk[Byte] => Decoder[T]#Decoded): Decoder[T] =
      (chunk: Chunk[Byte]) => fn(chunk)
  }

  trait Encoder[T] {
    def encode(t: T): Chunk[Byte]
  }
  object Encoder {
    def apply[T: Encoder]: Encoder[T]                 = implicitly[Encoder[T]]
    def instance[T](fn: T => Chunk[Byte]): Encoder[T] = (t: T) => fn(t)
  }

  trait Codec[T] extends Decoder[T] with Encoder[T]
  object Codec {
    def from[T](decoder: Decoder[T], encoder: Encoder[T]): Codec[T] =
      new Codec[T] {
        def encode(t: T): Chunk[Byte]                      = encoder.encode(t)
        def decode(bytes: Chunk[Byte]): Decoder[T]#Decoded = decoder.decode(bytes)
      }
  }
}
