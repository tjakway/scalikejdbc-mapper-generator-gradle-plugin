package org.grimrose.gradle.scalikejdbc.interop

import org.gradle.api.tasks.Input

import scala.beans.BeanProperty

class JdbcConfig {
  @BeanProperty
  @Input
  var driver: String = _
  @BeanProperty
  @Input
  var url: String = _
  @BeanProperty
  @Input
  var username: String = _
  @BeanProperty
  @Input
  var password: String = _
  @BeanProperty
  @Input
  var schema: String = _
}

object JdbcConfig extends ToProperties[JdbcConfig] {
  override def toMap(in: JdbcConfig): Map[String, String] = {
    import org.grimrose.gradle.scalikejdbc.mapper.ScalikeJDBCMapperGenerator.Keys._
    Map(
      JDBC_DRIVER -> in.driver,
      JDBC_URL -> in.url,
      JDBC_USER_NAME -> in.username,
      JDBC_PASSWORD -> in.password,
      JDBC_SCHEMA -> in.schema
    )
  }
}