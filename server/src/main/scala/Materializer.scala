import binary.Tftp.{Data, RRQ, TftpPacket, Undefined, WRQ}
import fs2.{Chunk, Pipe}
import cats.syntax.traverse._
import cats.syntax.validated._
import binary.Codec.syntax._
import cats.effect.Sync
import fs2.io.udp.Packet

trait Materializer[F[_], A, B] {
  def materialize: Pipe[F, A, B]
}

object Materializer extends TftpMaterializer {
  def apply[F[_], In, Out](implicit m: Materializer[F, In, Out]) = m
}

trait TftpMaterializer {
  implicit def tftpMaterializer[F[_]: Sync] =
    new Materializer[F, Chunk[Packet], Chunk[TftpPacket]] {
      def materialize: Pipe[F, Chunk[Packet], Chunk[TftpPacket]] = { in =>
        in.evalMap { packets =>
          packets
            .map { p =>
              (
                p.bytes.extractOpcode().code match {
                  case 1 => p.bytes.as[RRQ]
                  case 2 => p.bytes.as[WRQ]
                  case 3 => p.bytes.as[Data]
                  case _ => Undefined("wrong opcode value").invalid
                }
              ).merge -> p.remote
            }
            .traverse {
              case (packet, from) =>
                // todo log
                Sync[F].pure(packet)
            }
        }
      }
    }
}
