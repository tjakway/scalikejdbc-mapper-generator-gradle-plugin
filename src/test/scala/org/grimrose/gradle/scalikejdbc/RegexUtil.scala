package org.grimrose.gradle.scalikejdbc

import java.util.regex.Pattern

trait RegexUtil {
  protected def defaultFlags: Option[Int] = None
  protected def compile(rgx: String,
                        flags: Option[Int] = defaultFlags): Pattern = {
    flags match {
      case Some(f) => Pattern.compile(rgx, f)
      case None => Pattern.compile(rgx)
    }
  }

  protected def compile(rgx: String, flags: Int): Pattern =
    compile(rgx, Some(flags))

  val matchSingleLineCommentDecl: String = """//"""

  val matchMultiLineCommentStart: String = """/\\*"""
  val matchMultiLineCommentEnd: String = """\\*/"""
  val matchMultiLineComment: String =
    matchMultiLineCommentStart + """.*""" + matchMultiLineCommentEnd

  val matchCommentOrWhitespace: String =
    String.format("""(%s\\R|%s|\s)""",
      matchSingleLineCommentDecl, matchMultiLineComment)

  /**
   * WARNING: java 8+ ONLY
   */
  val matchLineBreak: String = """\R"""
}

object RegexUtil extends RegexUtil