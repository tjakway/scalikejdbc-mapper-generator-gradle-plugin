package org.grimrose.gradle.scalikejdbc.util

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

  def joinWithComma[A](xs: Iterable[A],
                       printF: A => String = callToString): String =
    joinWithSeparator[A](xs, ", ", printF)
}
