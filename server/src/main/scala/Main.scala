import cats.syntax.functor._
import cats.effect.{Blocker, ExitCode, IO, IOApp}
import fs2._
import org.slf4j.{Logger, LoggerFactory}

class Main extends IOApp {
  implicit val logger: Logger = LoggerFactory.getLogger(getClass)

  def run(args: List[String]): IO[ExitCode] =
    server.compile.drain.as(ExitCode.Success)

  def server: Stream[IO, Unit] =
    for {
      config  <- Stream.eval(Config.loadF[IO])
      blocker <- Stream.resource(Blocker[IO])
      serve   <- Server[IO].start(blocker, config)
    } yield serve
}
