import java.nio.ByteBuffer
import java.nio.charset.{Charset, StandardCharsets}

import fs2.Chunk

final case class Buffer private (private val underlying: Array[Byte]) { self =>
  private def contramap(f: ByteBuffer => ByteBuffer) =
    Buffer(f(ByteBuffer.wrap(underlying)).array())

  def put(number: Int): Buffer = self.contramap(_.putShort(number.toShort))
  def put(str: String, enc: Charset = StandardCharsets.UTF_8): Buffer =
    self.contramap(_.put(str.getBytes(enc)))
  def put(byteArray: Array[Byte]) = self.contramap(_.put(byteArray))
  def tombstone(): Buffer         = self.contramap(_.put(0.toByte))

  def toChunk: Chunk[Byte] = Chunk.array(underlying)
}

object Buffer {
  def empty                       = Buffer(Array.empty[Byte])
  def withCapacity(capacity: Int) = Buffer(Array.ofDim(capacity))
}
