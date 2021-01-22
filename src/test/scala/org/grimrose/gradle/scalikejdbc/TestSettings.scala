package org.grimrose.gradle.scalikejdbc

trait TestSettings {
  def getAvailableSQLDrivers: TestSettings.DriverMap =
    Map(
      "org.h2.Driver" -> "h2"
    )
}

object TestSettings {
  /**
   * a map of driver classes to their jdbc URL prefix
   */
  type DriverMap = Map[String, String]

  object Constants {
    val pluginPackageName: String = "org.grimrose.gradle.scalikejdbc"
  }
}
