import java.net.InetSocketAddress

import scala.concurrent.duration._
import cats.effect.{Blocker, Concurrent, ContextShift, Resource}
import fs2._
import fs2.io.udp.{Socket, SocketGroup}
import org.slf4j.Logger

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
      def socket: Resource[F, Socket[F]] =
        SocketGroup[F](blocker).flatMap(
            _.open[F](
              address = new InetSocketAddress(params.port.serverPort)
            , receiveBufferSize = Some(params.rcvBuffer.value)
            , sendBufferSize = Some(params.sndBuffer.value)
          )
        )

      Stream
        .resource(socket)
        .flatMap(_.reads(Some(params.timeout.duration.millis)))
        .chunks
        .through(new Materializer[F].apply)

      ???
    }
}
