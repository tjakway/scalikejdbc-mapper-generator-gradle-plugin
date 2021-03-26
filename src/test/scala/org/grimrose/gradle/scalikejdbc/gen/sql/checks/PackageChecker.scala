package org.grimrose.gradle.scalikejdbc.gen.sql.checks

import org.grimrose.gradle.scalikejdbc.RegexUtil
import org.grimrose.gradle.scalikejdbc.gen.sql.FailedCheck
import org.grimrose.gradle.scalikejdbc.gen.sql.checks.RegexChecker.CheckFunction

import java.util.regex.Pattern

case class PackageChecker(packageName: String)
  extends RegexChecker {
  import PackageChecker._

  private object Regexes extends RegexUtil {
    private val flags: Int = Pattern.DOTALL | Pattern.MULTILINE
    private def compile(x: String): Pattern = Pattern.compile(x, flags)

    private def mkPackageRegex(packageString: String): String = {
      String.format("""%s*(?!(%s|%s))package\s+%s\s*;?\R.*""",
        matchCommentOrWhitespace,
        matchSingleLineCommentDecl,
        matchMultiLineCommentStart,
        packageString)
    }

    val noPackageName: Pattern = compile(mkPackageRegex(""))

    /**
     * A package hierarchy of just 1 directory
     */
    val thinPackageHierarchy: Pattern =
      compile(mkPackageRegex("""\p{Alpha}[\p{Alnum}_]*"""))

    val noPackageDecl: Pattern = {
      val packageDeclRegex: String =
        mkPackageRegex("""\p{Alpha}(\p{Alnum}|(\\.|[\p{Alnum}_]))*""")

      compile(packageDeclRegex)
    }
  }
  import Regexes._

  override protected lazy val checkFunctions: Seq[CheckFunction] = Seq(
    warnIfMatch(thinPackageHierarchy, ThinPackageHierarchy.apply),
    errorIfMatch(noPackageName, NoPackageName.apply),
    errorIfMatch(noPackageDecl, NoPackageDeclaration.apply)
  )
}

object PackageChecker {
  class PackageCheckerError(override val msg: String)
    extends FailedCheck(msg)

  case class NoPackageDeclaration(override val msg: String)
    extends PackageCheckerError(msg)

  case class NoPackageName(override val msg: String)
    extends PackageCheckerError(msg)

  case class WrongPackageDeclaration(expected: String)
    extends PackageCheckerError(s"Expected package declaration $expected")

  case class ThinPackageHierarchy(override val msg: String)
    extends PackageCheckerError(msg)
}