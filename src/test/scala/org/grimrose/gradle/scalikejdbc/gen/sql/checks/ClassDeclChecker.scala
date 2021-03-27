package org.grimrose.gradle.scalikejdbc.gen.sql.checks

import org.grimrose.gradle.scalikejdbc.RegexUtil
import org.grimrose.gradle.scalikejdbc.gen.sql.FailedCheck
import org.grimrose.gradle.scalikejdbc.gen.sql.checks.RegexChecker.CheckFunction

import java.util.regex.Pattern

class ClassDeclChecker(val className: String,
                       val checkShortIdentifiers: Boolean = true)
  extends RegexChecker {
  import ClassDeclChecker._

  private object Regexes extends RegexUtil {
    override protected def defaultFlags: Option[Int] =
      Some(Pattern.DOTALL | Pattern.MULTILINE)

    private def matchIdentifier(quantifier: String): String =
      String.format("""\p{Alpha}[\p{Alnum}_\\$]%s""", quantifier)

    private val declKeywords: String = """(case\s+)?(class|object)"""

    lazy val declPattern: Pattern =
      compile(String.format("""%s\s+%s""",
        declKeywords, matchIdentifier("""*""")))

    /**
     * warn if a generated identifier is below this length
     */
    val tooShortThreshold: Int = 2
    lazy val shortIdentifierPattern: Pattern = {
      //doesn't make sense to warn if they have a negative length identifier
      assert(tooShortThreshold > 1)

      //because the pattern begins with a \p{Alpha}, the quantifier
      //is only over the remaining chars
      val actual = tooShortThreshold - 1
      compile(String.format("""%s\s+%s""",
        declKeywords, matchIdentifier(
          String.format("""{0,%s}""", actual.toString))))
    }
  }
  import Regexes._

  override protected def checkFunctions: Seq[CheckFunction] = Seq(
    warnIfMatch(shortIdentifierPattern, ClassNameTooShort.apply),
    errorIfNotMatch(declPattern, NoClassDeclaration.apply)
  )
}

object ClassDeclChecker {
  class ClassDeclCheckerError(override val msg: String)
    extends FailedCheck(msg)

  case class ClassNameTooShort(override val msg: String)
    extends FailedCheck(msg)

  case class NoClassDeclaration(override val msg: String)
    extends FailedCheck(msg)
}