import Codec.Decoded
import fs2.Pipe
import fs2.io.udp.Packet

trait Materializer[F[_], A, B] {
  def materialize: Pipe[F, A, B]
}

object Materializer extends TftpMaterializer {
  def apply[F[_], In, Out](implicit m: Materializer[F, In, Out]) = m
}

trait TftpMaterializer {
  implicit def tftpMaterializer[F[_]] =
    new Materializer[F, Packet, TftpPacket] {
      type Opcode = Int

      def materialize: Pipe[F, Packet, TftpPacket] = {
        val decodeMatcher: Opcode => Decoded[TftpPacket] = {
          case 1 => ???
          case 2 => ???
          case 3 => ???
          case 4 => ???
          case _ => ???
        }

        in =>
          in.map { packet =>
            // TODO take first 2 bytes as Short instance and pass to decode matcher
            ???
          }
      }
    }
}
