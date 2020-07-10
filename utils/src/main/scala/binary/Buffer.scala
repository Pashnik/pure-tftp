package binary

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import fs2.Chunk
import io.estatico.newtype.ops._
import io.estatico.newtype.Coercible
import scala.language.implicitConversions

final case class Buffer private (private val underlying: Array[Byte]) { self =>

  /**
    * Not referential transperent. The state is updated with with every function call.
    */
  trait BufferIterableOnce {
    def string(enc: Charset = UTF_8): String
    def short[B](implicit ec: Coercible[Short, B]): B
    def raw(): Chunk[Byte]
  }

  final private class IterableOnceImpl extends BufferIterableOnce {
    private val bf = ByteBuffer.wrap(underlying)

    def string(enc: Charset = UTF_8): String = {
      val pos = bf.position()
      val res = bf.array
        .slice(pos, bf.capacity + 1)
        .takeWhile(_ != 0)
      bf.position(pos + res.length + 1)
      new String(res, UTF_8)
    }

    def raw(): Chunk[Byte] = {
      val pos = bf.position()
      Chunk.array(
        bf.array()
          .slice(pos, bf.capacity() + 1))
    }

    def short[B](implicit ec: Coercible[Short, B]): B = bf.getShort.coerce
  }

  private def contramap(f: ByteBuffer => ByteBuffer) =
    Buffer(f(ByteBuffer.wrap(underlying)).array())

  def put(short: Short): Buffer = self.contramap(_.putShort(short))
  def put(str: String, enc: Charset = UTF_8): Buffer =
    self.contramap(_.put(str.getBytes(enc)))
  def put(byteArray: Array[Byte])      = self.contramap(_.put(byteArray))
  def put(chunk: Chunk[Byte]): Buffer  = self.put(chunk.toArray) // todo why it needs result type?
  def tombstone()                      = self.contramap(_.put(0.toByte))
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
