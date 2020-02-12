import java.net.InetSocketAddress

import scala.concurrent.duration._
import cats.effect.{Blocker, ConcurrentEffect, ContextShift, Resource}
import fs2._
import fs2.io.udp.{Packet, Socket, SocketGroup}

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
  def apply[F[_]: ConcurrentEffect: ContextShift]: Server[F] =
    new Server[F] {
      override def start(blocker: Blocker, params: Config): Stream[F, Unit] = {
        def socket: Resource[F, Socket[F]] =
          SocketGroup[F](blocker).flatMap(
              _.open[F](
                address = new InetSocketAddress(params.port.value)
              , receiveBufferSize = Some(params.rcvBuffer.value)
              , sendBufferSize = Some(params.sndBuffer.value)
            )
          )

        def materialize[In] = Materializer[F, In, TftpPacket].materialize

        Stream
          .resource(socket)
          .flatMap { udpSocket =>
            udpSocket.reads(Some(params.timeout.duration.millis))
          }
          .through(materialize)

        ???
      }
    }
}
