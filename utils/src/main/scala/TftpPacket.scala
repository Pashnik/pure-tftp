import binary.Codec._
import binary.Buffer.BufferOps._
import Decoder.DecodedFailure
import binary.{Buffer, Opcode}
import cats.syntax.either._
import fs2.Chunk

/**
  * Represents various types of TFTP packet
  * @see [[https://tools.ietf.org/html/rfc1350]]
  */
sealed trait TftpPacket extends Product with Serializable {
  val opcode: Opcode
}
object TftpPacket {}

sealed trait Mode { val value: String }
object Netascii extends Mode { val value: String = "netascii" }
object Octet    extends Mode { val value: String = "octet"    }
object Mail     extends Mode { val value: String = "mode"     }

object Mode { val modes = Netascii :: Octet :: Mail :: Nil }

sealed abstract class IOPacket[M <: Mode](fileName: String, format: M) extends TftpPacket
case class RRQ(fileName: String, mode: Mode = Netascii) extends IOPacket(fileName, mode) {
  val opcode: Opcode = Opcode.unsafe(1)
}
object RRQ {
  implicit val rrqCodec: Codec[RRQ] = Codec.from[RRQ](
      Decoder.instance { chunk =>
      val io                   = chunk.iterableOnce
      val (opcode, name, mode) = (io.short[Opcode], io.string(), io.string())

      (for {
        validOpcode <- Opcode.validate(opcode)
        validMode   <- Option.when(mode == Netascii.value)(mode)
      } yield validMode)
        .fold()
    }
    , Encoder.instance[RRQ](
        r =>
        Buffer
          .withCapacity(2 + r.fileName.length + 1 + r.mode.value.length + 1)
          .put(r.opcode)
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
  implicit val encoder: Encoder[WRQ] = (wrq: WRQ) => ???
  implicit val decoder: Decoder[WRQ] = (chunk: Chunk[Byte]) => ???
}

case class Block(number: Short)
case class Data(block: Block, data: Array[Byte]) extends TftpPacket {
  val opcode: Opcode = Opcode.unsafe(3)
}
object Data {
  implicit val encoder: Encoder[Data] = (data: Data) => ???
  implicit val decoder: Decoder[Data] = (chunk: Chunk[Byte]) => ???
}
case class Acknowledgment(block: Block) extends TftpPacket {
  val opcode: Opcode = Opcode.unsafe(4)
}
object Acknowledgment {
  implicit val encoder: Encoder[Acknowledgment] = (ack: Acknowledgment) => ???
  implicit val decoder: Decoder[Acknowledgment] = (bytes: Chunk[Byte]) => ???
}
