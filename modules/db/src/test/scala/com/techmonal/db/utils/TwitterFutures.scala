package com.techmonal.db.utils

import com.twitter.util.Awaitable
import org.scalatest.concurrent.Futures

import java.util.concurrent.TimeUnit
import scala.language.implicitConversions

trait TwitterFutures extends Futures {
  def await[T](a: Awaitable[T], nrSeconds: Long = 5): T =
    com.twitter.util.Await.result(a, com.twitter.util.Duration(nrSeconds, TimeUnit.SECONDS))

  implicit def convertScalaFuture[T](
      fut: com.twitter.util.Future[T]
  ): FutureConcept[T] = new FutureConcept[T] {

    override def eitherValue: Option[Either[Throwable, T]] = fut.poll.map {
      case com.twitter.util.Return(v)  => Right(v)
      case com.twitter.util.Throw(err) => Left(err)
    }

    override def isExpired: Boolean = false

    override def isCanceled: Boolean = false
  }

}
