package com.techmonal.db

import com.datastax.driver.core.querybuilder.QueryBuilder._
import com.datastax.driver.core.querybuilder._
import com.techmonal.db.Record.RecordMap

import scala.jdk.CollectionConverters._

final case class TableDetails(pkName: String, tableName: String, keyspace: String)

final case class ColumnDetails[T](columnName: String, makeRecord: T => Record)

final case class Schema[T](columnDetails: Seq[ColumnDetails[T]])(implicit tableDetails: TableDetails) {

  implicit class AnyOps[A](t: A) {
    def ~>[B](fn: A => B): B = fn(t)
  }

  def toRecordMap(t: T): RecordMap = columnDetails.foldLeft[RecordMap](Map())((map, cd) => map + (cd.columnName -> cd.makeRecord(t)))

  def buildInsertQuery(recordMap: RecordMap): Insert = buildInsert ~> addRecords(recordMap) ~> ifNotExist

  def buildSelectQuery[ID](id: ID): Select.Where = buildMapOfAllColumns ~> buildSelect ~> addWhere(id)

  def buildUpdateQuery(recordMap: RecordMap): Update.IfExists =
    buildUpdate ~> buildSet(recordMap) ~> addWhereWithUpdate(recordMap(tableDetails.pkName).raw) ~> ifExist

  def buildSelectQueryIn[ID](ids: Set[ID]): Select.Where = buildMapOfAllColumns ~> buildSelect ~> addWhere(ids)

  private def buildInsert: Insert = insertInto(tableDetails.keyspace, tableDetails.tableName)

  private def addRecords(recordMap: RecordMap)(insert: Insert): Insert = {
    val list = recordMap.toList.map { case (k, v) => (k, v.raw) }

    list.foldLeft[Insert](insert) { case (acc, (name, v)) =>
      acc.value(name, v)
    }
  }

  private def ifNotExist(insert: Insert): Insert = insert.ifNotExists()

  private def ifExist(update: Update.Where): Update.IfExists = update.ifExists()

  private def buildMapOfAllColumns: Select.Selection = columnDetails.foldLeft(select())((s, v) => s.column(v.columnName))

  private def buildSelect(s: Select.Selection): Select = s.from(tableDetails.keyspace, tableDetails.tableName)

  private def buildUpdate: Update = update(tableDetails.keyspace, tableDetails.tableName)

  private def buildSet(cassandraMap: RecordMap)(update: Update): Update.Assignments = {
    val list: List[(String, Any)] = cassandraMap.toList.collect { case (k, v) if k != tableDetails.pkName => (k, v.raw) }
    val head :: tail              = list

    tail.foldLeft(update.`with`(set(head._1, head._2))) { case (acc, (name, value)) =>
      acc.and(set(name, value))
    }
  }

  private def addWhere[ID](id: ID)(s: Select): Select.Where = s.where(QueryBuilder.eq(tableDetails.pkName, id))

  private def addWhere[ID](ids: Set[ID])(s: Select): Select.Where = s.where(in(tableDetails.pkName, ids.toList.asJava))

  private def addWhereWithUpdate[ID](id: ID)(update: Update.Assignments): Update.Where = update.where(QueryBuilder.eq(tableDetails.pkName, id))

}

object Schema {
  def toColumnDetails[T, V](name: String, value: T => V)(implicit fn: V => Record): ColumnDetails[T] = ColumnDetails(name, value.andThen(fn))
}
