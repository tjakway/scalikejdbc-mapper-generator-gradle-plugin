package org.grimrose.gradle.scalikejdbc.gen.sql

class FailedCheck(override val msg: String)
  extends GenSQLException(msg)
