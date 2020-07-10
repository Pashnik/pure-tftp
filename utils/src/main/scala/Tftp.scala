import binary.Codec._
import binary.Buffer.BufferOps._
import binary.Codec.Decoder.{DataOverspend, Failure, WrongMode}
import binary.{Block, Buffer, Opcode}
import cats.data.Validated
import cats.syntax.functor._
import fs2.Chunk
import scala.util.Try

/**
  * Represents various types of TFTP packet
  * @see [[https://tools.ietf.org/html/rfc1350]]
  */
object Tftp {
  sealed trait TftpPacket extends Product with Serializable {
    val opcode: Opcode
  }

  private[Tftp] object TftpPacket {
    val shortLength     = 2
    val tombstoneLength = 1
    val maxChunkLength  = 512
  }

  import TftpPacket._

  sealed trait Mode         { val value: String }
  private[Tftp] object Mode { val modes = Netascii :: Octet :: Mail :: Nil }
  object Netascii extends Mode { val value: String = "netascii" }
  object Octet    extends Mode { val value: String = "octet"    }
  object Mail     extends Mode { val value: String = "mode"     }

  sealed abstract class IOPacket[M <: Mode](fileName: String, format: M) extends TftpPacket

  case class RRQ(fileName: String, mode: Mode = Netascii) extends IOPacket(fileName, mode) {
    val opcode: Opcode = Opcode.unsafe(1)
  }

  object RRQ {
    implicit val rrqCodec: Codec[RRQ] = Codec.from[RRQ](
        Decoder.instance { chunk =>
        val io              = chunk.iterableOnce
        val (_, name, mode) = (io.short[Opcode], io.string(), io.string())

        Validated
          .cond(mode == Netascii.value, mode, WrongMode)
          .as(RRQ(name))
      }
      , Encoder.instance[RRQ](
          r =>
          Buffer
            .withCapacity(
                shortLength + r.fileName.length + tombstoneLength + r.mode.value.length + tombstoneLength
            )
            .put(r.opcode.code) // actually could be r.opcode
            .put(r.fileName)
            .tombstone()
            .put(r.mode.value)
            .tombstone()
            .toChunk
      )
    )
  }

  case class WRQ(fileName: String, mode: Mode = Netascii) extends IOPacket(fileName, mode) {
    val opcode: Opcode = Opcode.unsafe(2)
  }

  object WRQ {
    implicit val wrqCodec: Codec[WRQ] = Codec.from[WRQ](
      Decoder[RRQ].emapTry[WRQ](rrq => Try(WRQ(rrq.fileName))),
      Encoder[RRQ].contramap[WRQ](wrq => RRQ(wrq.fileName)))
  }

  case class Data(block: Block, raw: Chunk[Byte]) extends TftpPacket {
    val opcode: Opcode = Opcode.unsafe(3)
  }

  object Data {
    implicit val dataCodec: Codec[Data] = Codec.from[Data](
        Decoder.instance[Data] { chunk =>
        val io                = chunk.iterableOnce
        val (_, block, bytes) = (io.short[Opcode], io.short[Block], io.raw())

        Validated
          .cond(bytes.size > maxChunkLength, bytes, DataOverspend)
          .map(Data(block, _))
      }
      , Encoder.instance[Data] { data =>
        Buffer
          .withCapacity(shortLength + shortLength + data.raw.size)
          .put(data.opcode.code)
          .put(data.block.number)
          .put(data.raw)
          .toChunk
      }
    )
  }

  case class Acknowledgment(block: Block) extends TftpPacket {
    val opcode: Opcode = Opcode.unsafe(4)
  }

  object Acknowledgment {
    implicit val ackCodec: Codec[Acknowledgment] = Codec.from[Acknowledgment](
        Decoder.instance[Acknowledgment] { chunk =>
        val io         = chunk.iterableOnce
        val (_, block) = (io.short[Opcode], io.short[Block])

        Validated.valid[Failure, Acknowledgment](Acknowledgment(block))
      }
      , Encoder.instance { (ack: Acknowledgment) =>
        Buffer
          .withCapacity(shortLength + shortLength)
          .put(ack.opcode.code)
          .put(ack.block.number)
          .toChunk
      }
    )
  }
}
