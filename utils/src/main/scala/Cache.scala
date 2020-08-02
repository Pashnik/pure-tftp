import java.util.concurrent.TimeUnit

import binary.TimeStampMillis
import cats.Functor
import cats.syntax.flatMap._
import io.estatico.newtype.ops._
import fs2._
import cats.effect.{Concurrent, Timer}
import cats.effect.concurrent.Ref
import cats.syntax.functor._
import org.slf4j.Logger
import scala.concurrent.duration._
import scala.collection.mutable

trait Cache[F[_], K, V] {
  def get(k: K): F[Option[V]]
  def put(k: K, v: V): F[V]
  def remove(k: K): F[Unit]
}

object TimeBasedCache {
  private def now[F[_]: Functor: Timer] =
    Timer[F].clock.realTime(TimeUnit.MILLISECONDS).map(_.coerce[TimeStampMillis])

  private type Cleanup[F[_], K, V] = Ref[F, Map[K, (V, TimeStampMillis)]] => Stream[F, Unit]

  private def timeBasedCache[F[_]: Concurrent: Timer, K, V](
      ref: Ref[F, Map[K, (V, TimeStampMillis)]],
      routine: Cleanup[F, K, V]): F[Cache[F, K, V]] =
    Stream
      .emit(
          new Cache[F, K, V] {
          def get(k: K): F[Option[V]] = ref.get.map(_.get(k).map(_._1))
          def put(k: K, v: V): F[V] =
            for {
              timestamp <- TimeBasedCache.now
              res       <- ref.modify(_.updated(k, v -> timestamp) -> v)
            } yield res
          def remove(k: K): F[Unit] = ref.update(_.removed(k))
        }
      )
      .covary[F]
      .concurrently(routine(ref))
      .compile
      .lastOrError

  private def cleanupRoutine[F[_]: Concurrent: Timer, K, V](
      cleanupTime: FiniteDuration,
      timeWindow: FiniteDuration): Cleanup[F, K, V] = { state =>
    Stream.awakeEvery[F](cleanupTime) >> Stream.eval(now[F]).flatMap { now =>
      Stream.eval(
          state.update { prev =>
          val newState = mutable.Map.empty[K, (V, TimeStampMillis)]
          prev.foreach {
            case (k, (v, insertedAt)) =>
              if ((now - insertedAt) > timeWindow.toMillis) ()
              else newState += k -> (v -> now)
          }
          newState.toMap
        }
      )
    }
  }

  def apply[F[_]: Concurrent: Timer, K, V](
      cleanup: FiniteDuration = 30.minutes,
      timeWindow: FiniteDuration = 20.minutes)(implicit L: Logger): F[Cache[F, K, V]] =
    Ref
      .of(Map.empty[K, (V, TimeStampMillis)])
      .flatMap(timeBasedCache[F, K, V](_, cleanupRoutine(cleanup, timeWindow)))
}
