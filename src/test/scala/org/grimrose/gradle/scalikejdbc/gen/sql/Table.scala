package org.grimrose.gradle.scalikejdbc.gen.sql

case class Table(name: String,
                 columns: Seq[Column]) {
  private def printColumns(columnSeparator: String): String = {
    if(columns.isEmpty) {
      ""
    } else {
      columns.map(_.print).reduce(_ + columnSeparator + _)
    }
  }

  private def printColumnSection(columnSeparator: String): String = {
    if(columns.isEmpty) {
      ""
    } else {
      "(" + printColumns(columnSeparator) + ")"
    }
  }

  def getCreateTableStatement(columnSeparator: String =
                              Table.defaultColumnSeparator): String = {
    String.format("CREATE TABLE %s%s",
      name, printColumnSection(columnSeparator))
  }
}

object Table {
  def defaultColumnSeparator: String = ", "
}