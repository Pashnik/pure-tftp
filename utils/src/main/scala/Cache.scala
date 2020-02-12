import scala.concurrent.duration._
import cats.effect.{IO, Sync}
import cats.effect.concurrent.Ref
import cats.syntax.functor._

trait Cache[F[_], K, V] {
  def getOption(k: K): F[Option[V]]
  def unsafeGet(k: K): F[V]
  def put(k: K, v: V): F[Unit]
  def remove(k: K): F[Unit]
}

private class TimeBasedCache[F[_], K, V](private val ref: Ref[F, Map[K, V]]) extends Cache[F, K, V] {
  def getOption(k: K): F[Option[V]] = ???
  def unsafeGet(k: K): F[V]         = ???
  def put(k: K, v: V): F[Unit]      = ???
  def remove(k: K): F[Unit]         = ???
}

object TimeBasedCache {
  def apply[F[_]: Sync, K, V](cleanup: FiniteDuration = 1.hour): F[TimeBasedCache[F, K, V]] =
    Ref.of(Map.empty[K, V]).map(new TimeBasedCache(_))
}
