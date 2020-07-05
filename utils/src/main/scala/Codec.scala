import fs2.Chunk

object Codec {
  type Decoded[T] = Either[DecodedFailure, T]

  trait Decoder[T] {
    def decode(bytes: Chunk[Byte]): Decoded[T]
  }

  trait Encoder[T] {
    def encode(tftpPacket: T): Chunk[Byte]
  }
}

sealed abstract class DecodedFailure(description: String) extends Product with Serializable
case object A                                             extends DecodedFailure("A")
case object B                                             extends DecodedFailure("B")
