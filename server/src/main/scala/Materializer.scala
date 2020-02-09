import Codec.{Decoded, Decoder}
import cats.effect.{ConcurrentEffect, ContextShift}
import cats.syntax.either._
import fs2.Pipe
import fs2.io.udp.Packet

trait Materializer[F[_], A, B] {
  def materialize: Pipe[F, A, B]
}

object Materializer {
  def apply[F[_]: ConcurrentEffect: ContextShift, A, B]: Materializer[F, A, B] = implicitly

  implicit def tftpMaterializer[F] =
    new Materializer[F, Packet, TftpPacket] {
      type Opcode = Int
      def materialize: Pipe[F, Packet, TftpPacket] = {

        // TODO can we do better ?
        val decodeMatcher: Opcode => Decoded[TftpPacket] = {
          case 1 => RRQ.decoder.decode
          case 2 => WRQ.decoder.decode
          case 3 => Data.decoder.decode
          case 4 => Acknowledgment.decoder.decode
          case _ => DecodedFailures.error.asLeft
        }

        in =>
          in.map { packet =>
            // TODO take first 2 bytes as Short instance and pass to decode matcher
            ???
          }
      }
    }
}
