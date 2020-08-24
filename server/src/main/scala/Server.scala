import java.net.InetSocketAddress

import binary.Tftp.{ACK, ErrorCode, IllegalOperator, RRQ, TftpPacket, WRQ}

import scala.concurrent.duration._
import cats.effect.{Blocker, Concurrent, ContextShift, Resource}
import fs2._
import fs2.io.udp.{Packet, Socket, SocketGroup}
import org.slf4j.Logger
import cats.syntax.option._
import binary.Codec.syntax._
import cats.syntax.validated._

trait Server[F[_]] {
  def start(blocker: Blocker, params: Config): Stream[F, Unit]
}

object Server {

  /**
    * Trying to increase the socket receive buffer for cases where
    * datagrams arrive in bursts faster than they can be processed.
    *
    * Send buffer size is actually size of the largest packet.
    */
  def apply[F[_]: Concurrent: ContextShift](implicit L: Logger): Server[F] =
    (blocker: Blocker, params: Config) => {
      val socketResource: Resource[F, Socket[F]] =
        SocketGroup[F](blocker).flatMap(
            _.open[F](
              address = new InetSocketAddress(params.port.serverPort)
            , receiveBufferSize = params.rcvBuffer.value.some
            , sendBufferSize = params.sndBuffer.value.some
          )
        )

      val decodingPipe: Pipe[F, Packet, (InetSocketAddress, TftpPacket)] =
        _.chunks
          .flatMap { chunk =>
            Stream.chunk(chunk.map { p =>
              p.remote -> (p.bytes.extractOpcode() match {
                case 1 => p.bytes.as[RRQ]
                case 2 => p.bytes.as[WRQ]
                case 3 => p.bytes.as[ACK]
                case _ =>
                  IllegalOperator("ho-ho").invalid[TftpPacket]
              }).merge
            })
          }

      val pipeline: Pipe[F, Packet, Packet] = ???

      Stream
        .resource(socketResource)
        .flatMap { serv =>
          serv
            .reads(params.timeout.duration.millis.some)
            .through(pipeline)
            .through(serv.writes(500.millis.some))
        }
    }
}
