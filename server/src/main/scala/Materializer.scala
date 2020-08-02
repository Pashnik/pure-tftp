import binary.Tftp.{Data, RRQ, TftpPacket, Undefined, WRQ}
import fs2.{Chunk, Pipe}
import cats.syntax.traverse._
import cats.syntax.validated._
import binary.Codec.syntax._
import cats.effect.Sync
import cats.syntax.apply._
import fs2.io.udp.Packet
import fs2._
import org.slf4j.Logger

class Materializer[F[_]](implicit S: Sync[F], L: Logger)
    extends Pipe[F, Chunk[Packet], Chunk[TftpPacket]] {
  def apply(in: Stream[F, Chunk[Packet]]): Stream[F, Chunk[TftpPacket]] =
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
            S.delay(
                L.info(s"from: ${from.getHostName}/${from.getAddress.getHostAddress}" + packet.show)
            ) *> S.pure(packet)
        }
    }
}
