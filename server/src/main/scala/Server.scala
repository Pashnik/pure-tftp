import java.net.{InetAddress, InetSocketAddress}

import binary.Tftp.{ACK, Data, IllegalOperator, RRQ, TftpPacket, WRQ}
import scala.concurrent.duration._
import cats.effect.{Blocker, Concurrent, ContextShift, Resource}
import fs2._
import fs2.io.udp.{Packet, Socket, SocketGroup}
import org.slf4j.Logger
import cats.syntax.option._
import binary.Codec.syntax._
import cats.syntax.functor._
import cats.syntax.validated._
import scala.collection.mutable

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
        _.mapChunks {
          _.map { p =>
            p.remote -> (p.bytes.extractOpcode() match {
              case 1 => p.bytes.as[RRQ]
              case 2 => p.bytes.as[WRQ]
              case 3 => p.bytes.as[Data]
              case 4 => p.bytes.as[ACK]
              case _ =>
                IllegalOperator("can't recognize tftp-operation").invalid[TftpPacket]
            }).merge
          }
        }

      val pipeline: Pipe[F, Packet, Packet] =
        _.through(decodingPipe).chunks
          .flatMap { ch =>
            val ips = mutable.Map.empty[InetAddress, TftpPacket]
            val res = ch.map {
              case (from, packet) =>
                ips
                  .get(from.getAddress) match {
                  case _: Some[TftpPacket] => ()
                  case None =>
                    ips += (from.getAddress -> packet)
                    ()
                }
                packet
            }

            Stream
              .eval(
                  Concurrent[F]
                  .delay(
                      ips.foreach {
                      case (from, packet) =>
                        L.info(s"received packet=${packet.show} from $from")
                    }
                  )
              ) >> Stream.chunk(res)
          }

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
