package org.grimrose.gradle.scalikejdbc.gen.sql

case class SQLDriver(driverClass: String,
                     jdbcPrefix: String,
                     sqlTypes: Set[SQLType])
