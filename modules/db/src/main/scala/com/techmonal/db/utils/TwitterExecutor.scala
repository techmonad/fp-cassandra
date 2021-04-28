package com.techmonal.db.utils

import com.datastax.driver.core.{ResultSet, Session, Statement}
import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}
import com.techmonal.db.{DBMonadicF, DBSession}
import com.twitter.util.{Future, Promise}

object TwitterExecutor {

  def toFuture(session: Session): DBMonadicF[Future] = (statement: Statement) => convertToFuture(session.executeAsync(statement))

  def futureSession(ks: String, session: Session): DBSession[Future] = new DBSession[Future] {
    def keySpace: String = ks
    def execute(statement: Statement): Future[ResultSet] =
      toFuture(session).execute(statement)
  }

  private def convertToFuture[T](lf: ListenableFuture[T]): Future[T] = {
    val p = Promise[T]
    Futures.addCallback(
      lf,
      new FutureCallback[T] {
        def onSuccess(result: T): Unit    = p.setValue(result)
        def onFailure(t: Throwable): Unit = p.setException(t)
      }
    )
    p
  }

}
