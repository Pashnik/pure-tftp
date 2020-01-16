import pureconfig._
import pureconfig.generic.auto._ // do not remove

final case class Port(value: Int)
final case class ReceiveBuffer(value: Int)
final case class SendBuffer(value: Int)
final case class Timeout(duration: Int)

case class Config(port: Port, rcvBuffer: ReceiveBuffer, sndBuffer: SendBuffer, timeout: Timeout)
object Config {
  private[this] val loader = ConfigSource.default.load[Config]

  def load: Either[Throwable, Config] = loader.left.map { flr =>
    new RuntimeException(s"errors occurred during loading config ${flr.toList.mkString}")
  }

  def unsafeLoad: Config = loader.getOrElse(throw new RuntimeException("Error by loading config"))
}
