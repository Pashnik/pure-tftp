import fs2.Chunk

class Codec {
  type Decoded[T] = Either[DecodedFailures, T]

  trait Decoder[+T] {
    def decode: Decoded[T]
  }

  trait Encoder[-T <: TftpPacket] {
    def encode(packet: T): Chunk[Byte]
  }
}

sealed trait DecodedFailures extends Product with Serializable
