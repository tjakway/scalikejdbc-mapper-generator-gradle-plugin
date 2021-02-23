package org.grimrose.gradle.scalikejdbc.gen.sql

abstract class SQLDriver(val driverClass: String,
                         val jdbcPrefix: String,
                         val sqlTypes: Set[SQLType],
                         val modifiers: Set[ColumnModifier]) {
  def getAutoincrementModifier: ColumnModifier
}
