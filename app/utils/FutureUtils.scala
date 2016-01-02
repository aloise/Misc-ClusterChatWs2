package utils


import scala.collection.generic.CanBuildFrom
import scala.concurrent.{ExecutionContext, Future}
/**
 * User: aloise
 * Date: 03.08.15
 * Time: 23:16
 */
object FutureUtils {

  def serialiseFutures[A, B, C[A] <: Iterable[A]]
  (collection: C[A])(fn: A ⇒ Future[B])(
    implicit ec: ExecutionContext,
    cbf: CanBuildFrom[C[B], B, C[B]]): Future[C[B]] = {
    val builder = cbf()
    builder.sizeHint(collection.size)

    collection.foldLeft(Future(builder)) {
      (previousFuture, next) ⇒
        for {
          previousResults ← previousFuture
          next ← fn(next)
        } yield previousResults += next
    } map { builder ⇒ builder.result }
  }

}
