package com.techmonal.db.utils

import cats.{Monad, MonadError}
import com.twitter.util.Future

import scala.util.control.NonFatal

object TwitterMonadicPredef {

  /**
   * A Monad instance for Twitter Future
   */
  implicit val monadTwitterFuture: Monad[Future] = new Monad[Future] {
    override def pure[A](x: A): Future[A] = Future.value(x)

    override def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] =
      fa.flatMap(f)

    override def tailRecM[A, B](a: A)(f: A => Future[Either[A, B]]): Future[B] =
      f(a).flatMap {
        case Left(v)  => tailRecM(v)(f)
        case Right(v) => Future.value(v)
      }
  }

  implicit val monadErrorTwitterFuture: MonadError[Future, Throwable] = new MonadError[Future, Throwable] {
    override def raiseError[A](e: Throwable): Future[A] = Future.exception(e)

    override def handleErrorWith[A](fa: Future[A])(f: Throwable => Future[A]): Future[A] = fa.rescue { case NonFatal(e) =>
      f(e)
    }

    override def pure[A](x: A): Future[A] = Future.value(x)

    override def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] =
      fa.flatMap(f)

    override def tailRecM[A, B](a: A)(f: A => Future[Either[A, B]]): Future[B] =
      f(a).flatMap {
        case Left(v)  => tailRecM(v)(f)
        case Right(v) => Future.value(v)
      }
  }

}
