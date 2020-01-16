import java.net.InetSocketAddress

import scala.concurrent.duration._
import cats.effect.{Blocker, ConcurrentEffect, ContextShift, Resource, Sync}
import fs2._
import fs2.io.udp.{Socket, SocketGroup}

object Server {

  /**
    * Trying to increase the socket receive buffer for cases where
    * datagrams arrive in bursts faster than they can be processed.
    *
    * Send buffer size is actually size of the largest packet.
    */
  def start[F[_]: Sync: ContextShift](blocker: Blocker, params: Config)(
        implicit F: ConcurrentEffect[F]
  ): Stream[F, Unit] = {
    def socket: Resource[F, Socket[F]] =
      SocketGroup[F](blocker).flatMap(
          _.open[F](
            address = new InetSocketAddress(params.port.value)
          , receiveBufferSize = Some(params.rcvBuffer.value)
          , sendBufferSize = Some(params.sndBuffer.value)
        )
      )

    Stream
      .resource(socket)
      .flatMap { udpSocket =>
        udpSocket.reads(Some(params.timeout.duration.millis))
      }
    ???
  }
}
