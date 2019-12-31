import cats.effect.{Blocker, ContextShift, Resource, Sync}
import fs2._
import fs2.io.udp.{Socket, SocketGroup}

object Server {
  def start[F[_]: Sync: ContextShift](blocker: Blocker): Stream[F, Unit] = {
    // TODO specify params for socketGroup.open
    def socket: Resource[F, Socket[F]] =
      SocketGroup[F](blocker).flatMap(_.open())

    Stream.resource(socket).map { socket =>
      ???
    }
  }
}
