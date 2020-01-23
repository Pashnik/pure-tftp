import fs2.Chunk

object Codec {
  type Decoded[T] = Either[DecodedFailures, T]

  trait Decoder[+T] {
    def decode: Decoded[T]
  }

  trait Encoder[-T] {
    def encode(tftpPacket: T): Chunk[Byte]
  }
}

sealed trait DecodedFailures extends Product with Serializable
