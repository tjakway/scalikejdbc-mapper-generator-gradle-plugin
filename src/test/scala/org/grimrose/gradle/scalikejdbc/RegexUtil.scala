package org.grimrose.gradle.scalikejdbc

trait RegexUtil {
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