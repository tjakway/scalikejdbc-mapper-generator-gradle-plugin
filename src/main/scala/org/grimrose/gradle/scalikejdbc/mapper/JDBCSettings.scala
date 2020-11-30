package org.grimrose.gradle.scalikejdbc.mapper

trait JDBCSettings {
  val driver: String
  val url: String
  val username: String
  val password: String
  val schema: String
}

object JDBCSettings {
  private case class JDBCSettingsImpl(
     driver: String, url: String, username: String, password: String, schema: String)
    extends JDBCSettings

  def apply(driver: String,
            url: String,
            username: String,
            password: String,
            schema: String): JDBCSettings =
    JDBCSettingsImpl(driver, url, username, password, schema)
}