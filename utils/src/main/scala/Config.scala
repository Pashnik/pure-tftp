import cats.MonadError
import pureconfig._
import pureconfig.generic.auto._ // do not remove

final case class Port(serverPort: Int)
final case class ReceiveBuffer(value: Int)
final case class SendBuffer(value: Int)
final case class Timeout(duration: Int)

case class Config(port: Port, rcvBuffer: ReceiveBuffer, sndBuffer: SendBuffer, timeout: Timeout)
object Config {
  def loadF[F[_]](implicit ME: MonadError[F, Throwable]): F[Config] =
    ME.fromEither[Config](
        ConfigSource.default.load[Config].left.map { flrs =>
        new RuntimeException(s"cannot load configuration, failures are: ${flrs.prettyPrint()}")
      }
    )
}
