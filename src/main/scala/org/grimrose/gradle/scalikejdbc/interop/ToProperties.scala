package org.grimrose.gradle.scalikejdbc.interop

import java.util.Properties
import java.util.Collection

import org.grimrose.gradle.scalikejdbc.util.Util

trait ToProperties[A] {
  def toProperties(in: A): Properties = {
    //filter null or empty values
    val map = toMap(in).filter {
      case (_, value) =>
        Option(value).exists(v => v.trim.nonEmpty)
    }

    Util.mapToProperties(map)
  }

  def formatCollection(x: Collection[String]): String =
    Util.joinWithSeparator(x, ",")

  def toMap(in: A): Map[String, String]
}
