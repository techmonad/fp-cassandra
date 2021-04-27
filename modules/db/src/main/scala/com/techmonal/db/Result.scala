package com.techmonal.db

import java.time.Instant
import java.util.UUID

import com.datastax.driver.core.Row
import io.circe.Decoder
import io.circe.parser.parse

import scala.util.{Failure, Try}

trait ResultBuilder[T] {
  def resultBuilder(row: Row): Try[T] = rowBuilder(new RowBuilder(row))
  def rowBuilder(builder: RowBuilder): Try[T]
}

class RowBuilder(row: Row) {

  private def withError[T](f: String => T)(key: String): Try[T] =
    Try(f(key)) match {
      case Failure(exception) => Failure(new Exception(s"Not able to fetch $key in $row because of exception", exception))
      case etc                => etc
    }

  def getString: String => Try[String] = withError(row.getString)

  def getInt: String => Try[Int] = withError(row.getInt)

  def getBool: String => Try[Boolean] = withError(row.getBool)

  def getDouble: String => Try[Double] = withError(row.getDouble)

  def getDate: String => Try[Instant] = name => withError(row.getTimestamp)(name).map(_.toInstant)

  def getUUID: String => Try[UUID] = withError(row.getUUID)

  def getJson[T](name: String)(implicit d: Decoder[T]): Try[T] =
    for {
      value  <- getString(name)
      parsed <- parse(value).toTry
      v      <- parsed.as[T].toTry
    } yield v

  def getJsonOpt[T](name: String)(implicit d: Decoder[T]): Try[Option[T]] = getString(name).flatMap { value =>
    parse(value).toOption match {
      case Some(parsed) => Try(parsed.as[T].toOption)
      case None         => Try(None)
    }
  }
}
