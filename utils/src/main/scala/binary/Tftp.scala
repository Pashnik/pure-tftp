package binary

import binary.Buffer.BufferOps._
import binary.Codec._
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
    def show: String
  }

  private[Tftp] object TftpPacket {
    val numberLength    = 2 // Short
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
    def show: String   = s"type=RRQ; file-name=$fileName; mode=$mode"
  }

  object RRQ {
    implicit val rrqCodec: Codec[RRQ] = Codec.from(
        Decoder.instance { chunk =>
        val io              = chunk.iterableOnce
        val (_, name, mode) = (io.number[Opcode](), io.string(), io.string())

        Validated
          .cond[ErrorCode, String](mode == Netascii.value,
                                   mode,
                                   Undefined("Mode should be netascii"))
          .as(RRQ(name))
      }
      , Encoder.instance[RRQ](
          r =>
          Buffer
            .withCapacity(
                numberLength + r.fileName.length + tombstoneLength + r.mode.value.length + tombstoneLength
            )
            .put(r.opcode.code) // actually could be r.opcode
            .put(r.fileName)
            .withTombstone
            .put(r.mode.value)
            .withTombstone
            .toChunk
      )
    )
  }

  case class WRQ(fileName: String, mode: Mode = Netascii) extends IOPacket(fileName, mode) {
    val opcode: Opcode = Opcode.unsafe(2)
    def show: String   = s"type=WRQ; file-name=$fileName; mode=$mode"
  }

  object WRQ {
    implicit val wrqCodec: Codec[WRQ] = Codec.from(
      Decoder[RRQ].emapTry[WRQ](rrq => Try(WRQ(rrq.fileName))),
      Encoder[RRQ].contramap[WRQ](wrq => RRQ(wrq.fileName)))
  }

  case class Data(block: Block, raw: Chunk[Byte]) extends TftpPacket {
    val opcode: Opcode = Opcode.unsafe(3)
    def show: String   = s"type=Data; block=${block.number}"
  }

  object Data {
    implicit val dataCodec: Codec[Data] = Codec.from(
        Decoder.instance[Data] { chunk =>
        val io                = chunk.iterableOnce
        val (_, block, bytes) = (io.number[Opcode](), io.number[Block](), io.raw())

        Validated
          .cond[ErrorCode, Chunk[Byte]](bytes.size < maxChunkLength,
                                        bytes,
                                        Undefined("Chunk size is more then 512 bytes"))
          .map(Data(block, _))
      }
      , Encoder.instance[Data] { data =>
        Buffer
          .withCapacity(numberLength + numberLength + data.raw.size)
          .put(data.opcode.code)
          .put(data.block.number)
          .put(data.raw)
          .toChunk
      }
    )
  }

  case class Acknowledgment(block: Block) extends TftpPacket {
    val opcode: Opcode = Opcode.unsafe(4)
    def show: String   = s"type=ACK; block=${block.number}"
  }

  object Acknowledgment {
    implicit val ackCodec: Codec[Acknowledgment] = Codec.from(
        Decoder.instance[Acknowledgment] { chunk =>
        val io         = chunk.iterableOnce
        val (_, block) = (io.number[Opcode](), io.number[Block]())

        Validated.valid[ErrorCode, Acknowledgment](Acknowledgment(block))
      }
      , Encoder.instance { (ack: Acknowledgment) =>
        Buffer
          .withCapacity(numberLength + numberLength)
          .put(ack.opcode.code)
          .put(ack.block.number)
          .toChunk
      }
    )
  }

  sealed abstract class ErrorCode(val description: String, val message: String, val code: Code)
      extends TftpPacket {
    val opcode: Opcode = Opcode.unsafe(5)
    def show: String   = s"type=Error; description=$description"
  }
  final case class Undefined(override val message: String)
      extends ErrorCode("Not defined, see error message", message, Code(1)) {}
  final case class FileNotFound(override val message: String)
      extends ErrorCode("File not found", message, Code(2))
  final case class IllegalOperator(override val message: String)
      extends ErrorCode("Illegal TFTP operation", message, Code(3))
  final case class FileAlreadyExists(override val message: String)
      extends ErrorCode("File already exists at server file system", message, Code(4))

  object ErrorCode {
    val allCodes =
      Map(Code(1) -> Undefined,
          Code(2) -> FileNotFound,
          Code(3) -> IllegalOperator,
          Code(4) -> FileAlreadyExists)

    implicit val errorCodeCodec: Codec[ErrorCode] = Codec.from(
        Decoder.instance[ErrorCode] { chunk =>
        val io                 = chunk.iterableOnce
        val (_, code, message) = (io.number[Opcode](), io.number[Code](), io.string())

        Validated
          .fromOption(allCodes
                        .get(code)
                        .map(f => f(message)),
                      Undefined("wrong error code number"))
      }
      , Encoder.instance[ErrorCode] { error =>
        Buffer
          .withCapacity(numberLength + numberLength + error.message.length + tombstoneLength)
          .put(error.opcode.code)
          .put(error.code.code)
          .put(error.message)
          .withTombstone
          .toChunk
      }
    )
  }
}
