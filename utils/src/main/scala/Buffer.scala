import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8

import TftpPacket.Opcode
import fs2.Chunk

import scala.language.implicitConversions

final case class Buffer private (private val underlying: Array[Byte]) { self =>
  trait BufferIterableOnce {
    def takeString(enc: Charset = UTF_8): String
    def takeOpcode: Opcode
    def takeBlock: Block
    def takeErrorCode: Int
  }

  final private class IterableOnceImpl extends BufferIterableOnce {
    private val bf = ByteBuffer.wrap(underlying)

    def takeString(enc: Charset = UTF_8): String = {
      val pos = bf.position()
      val res = bf.array
        .slice(pos, bf.capacity + 1)
        .takeWhile(_ != 0)
      bf.position(pos + res.length + 1)
      new String(res, UTF_8)
    }

    def takeOpcode: Opcode = ???
    def takeBlock: Block   = Block(bf.getShort)
    def takeErrorCode: Int = ???
  }

  private def contramap(f: ByteBuffer => ByteBuffer) =
    Buffer(f(ByteBuffer.wrap(underlying)).array())

  def put(number: Int): Buffer = self.contramap(_.putShort(number.toShort))
  def put(str: String, enc: Charset = UTF_8): Buffer =
    self.contramap(_.put(str.getBytes(enc)))
  def put(byteArray: Array[Byte])      = self.contramap(_.put(byteArray))
  def tombstone(): Buffer              = self.contramap(_.put(0.toByte))
  def toChunk: Chunk[Byte]             = Chunk.array(underlying)
  def iterableOnce: BufferIterableOnce = new IterableOnceImpl
}

object Buffer {
  def empty                         = Buffer(Array.empty[Byte])
  def withCapacity(capacity: Int)   = Buffer(Array.ofDim(capacity))
  def fromChunk(chunk: Chunk[Byte]) = Buffer(chunk.toArray)

  object BufferOps {
    implicit def chunkToBuffer(chunk: Chunk[Byte]): Buffer = Buffer.fromChunk(chunk)
  }
}
