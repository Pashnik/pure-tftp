import fs2.Chunk

object Codec {
  type Decoded[T] = Either[DecodedFailure, T]

  trait Decoder[+T] {
    def decode: Decoded[T]
  }

  trait Encoder[-T] {
    def encode(tftpPacket: T): Chunk[Byte]
  }
}

// TODO Maybe typeclass

sealed trait DecodedFailure extends Product with Serializable

object DecodedFailures {
  def error: DecodedFailure = ???
}
