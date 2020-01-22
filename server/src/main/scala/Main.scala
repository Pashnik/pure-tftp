import cats.effect.{Blocker, ExitCode, IO, IOApp}
import fs2._
import cats.syntax.functor._

class Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    server.compile.drain.as(ExitCode.Success)

  def server: Stream[IO, Unit] =
    for {
      config  <- Stream.eval(IO.fromEither(Config.load))
      blocker <- Stream.resource(Blocker[IO])
      serve   <- Server[IO].start(blocker, config)
    } yield serve
}
