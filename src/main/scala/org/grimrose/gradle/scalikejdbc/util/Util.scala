package org.grimrose.gradle.scalikejdbc.util

import java.util.Properties

import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters

object Util {
  private def callToString[A]: A => String = (a: A) => a.toString
  def joinWithSeparator[A](xs: Iterable[A],
                           separator: String,
                           printF: A => String = callToString): String = {
    if(xs.isEmpty) {
      ""
    } else {
      val sb = new java.lang.StringBuilder()
      xs.foreach { x =>
        sb.append(printF(x))
        sb.append(separator)
      }

      val joinedStr = sb.toString

      //dont need to remove the last separator if there isn't one
      if(separator.nonEmpty) {
        val index = joinedStr.lastIndexOf(separator)
        //we inserted at least one separator so lastIndexOf should
        //never be negative
        assert(index > 0)
        assert(joinedStr.length > (index - 1))
        joinedStr.substring(0, index)
      } else {
        joinedStr
      }
    }
  }

  def joinWithSeparator[A](xs: java.util.Collection[A],
                           separator: String): String =
    joinWithSeparator(xs, separator, callToString[A])

  def joinWithSeparator[A](xs: java.util.Collection[A],
                           separator: String,
                           printF: A => String): String = {
    val iterable: Iterable[A] =
      JavaConverters.asScalaIterator(xs.iterator()).toIterable
    joinWithSeparator(iterable, separator, printF)
  }

  def joinWithComma[A](xs: Iterable[A],
                       printF: A => String = callToString): String =
    joinWithSeparator[A](xs, ", ", printF)

  def mapToProperties(xs: Map[String, String]): Properties = {
    val props = new Properties()
    xs.foreach {
      case (key, value) => props.setProperty(key, value)
    }

    if(xs.nonEmpty) {
      assert(!props.isEmpty)
    }

    props
  }
}
