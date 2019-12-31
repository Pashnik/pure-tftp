import cats.effect.{Blocker, ExitCode, IO, IOApp}
import fs2._
import cats.syntax.functor._

class Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    withBlocker
      .flatMap { blocker =>
        Server
          .start[IO](blocker)
      }
      .compile
      .drain
      .as(ExitCode.Success)

  def withBlocker: Stream[IO, Blocker] =
    Stream.resource(Blocker[IO])
}
