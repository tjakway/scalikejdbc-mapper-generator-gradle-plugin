package org.grimrose.gradle.scalikejdbc.gen.sql

abstract class ColumnModifier(val sqlRep: String)

object ColumnModifier {
  case object PrimaryKey extends ColumnModifier("PRIMARY KEY")
  case object Unique extends ColumnModifier("UNIQUE")
  case object NotNull extends ColumnModifier("NOT NULL")

  def unapply(c: ColumnModifier): Option[String] = Some(c.sqlRep)
}
