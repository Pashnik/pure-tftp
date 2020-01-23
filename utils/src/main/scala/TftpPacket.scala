import java.io.File

import Codec._
import fs2.Chunk

/**
  * Represents various types of TFTP packet
  * @see [[https://tools.ietf.org/html/rfc1350]]
  */
sealed trait TftpPacket extends Product with Serializable

sealed trait Mode { val value: String }
object Netascii extends Mode { val value: String = "netascii" }
object Octet    extends Mode { val value: String = "octet"    }
object Mail     extends Mode { val value: String = "mode"     }

object Mode { val modes = List(Netascii, Octet, Mail) }

sealed abstract case class IOPacket[T <: Mode](path: File, format: T) extends TftpPacket
case class RRQ(file: File, mode: Mode = Netascii)                     extends IOPacket(file, mode) { val opcode = 1 }
object RRQ {
  implicit val encoder: Encoder[RRQ] = (rrq: RRQ) => ???
  implicit val decoder: Decoder[RRQ] = (chunk: Chunk[Byte]) => ???
}
case class WRQ(file: File, mode: Mode = Netascii) extends IOPacket(file, mode) { val opcode = 2 }
object WRQ {
  implicit val encoder: Encoder[WRQ] = (wrq: WRQ) => ???
  implicit val decoder: Decoder[WRQ] = (chunk: Chunk[Byte]) => ???
}

case class Block(number: Short)
case class Data(block: Block, data: Array[Byte]) extends TftpPacket { val opcode = 3 }
object Data {
  implicit val encoder: Encoder[Data] = (data: Data) => ???
  implicit val decoder: Decoder[Data] = (chunk: Chunk[Byte]) => ???
}
case class Acknowledgment(block: Block) extends TftpPacket { val opcode = 4 }
object Acknowledgment {
  implicit val encoder: Encoder[Acknowledgment] = (ack: Acknowledgment) => ???
  implicit val decoder: Decoder[Acknowledgment] = (bytes: Array[Byte]) => ???
}
