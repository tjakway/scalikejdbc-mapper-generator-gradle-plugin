package org.grimrose.gradle.scalikejdbc.gen.sql

case class Column(name: String,
                  sqlType: SQLType,
                  modifiers: Set[ColumnModifier])
