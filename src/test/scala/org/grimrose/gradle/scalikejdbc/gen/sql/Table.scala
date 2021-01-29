package org.grimrose.gradle.scalikejdbc.gen.sql

case class Table(name: String,
                 columns: Seq[Column]) {
  //TODO
  def getCreateTableStatement: String = ???
}

object Table {
  def
}