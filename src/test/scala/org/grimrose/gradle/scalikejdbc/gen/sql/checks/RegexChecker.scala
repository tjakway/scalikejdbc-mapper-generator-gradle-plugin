package org.grimrose.gradle.scalikejdbc.gen.sql.checks

import org.grimrose.gradle.scalikejdbc.TestSettings
import org.grimrose.gradle.scalikejdbc.gen.sql.checks

import java.io.File
import java.util.regex.Pattern

abstract class RegexChecker
  extends checks.OutputChecker
    with RegexChecker.MkCheckFunctions {
  import RegexChecker._

  protected def checkFunctions: Seq[CheckFunction]

  override def apply(file: File): OutputChecker.Result = wrapExceptions {
    def read(f: File): String = {
      import scala.io.Source
      var source: Option[Source] = None

      try {
        source = Some(scala.io.Source.fromFile(
          f, TestSettings.Constants.encoding))
        source.mkString
      } finally {
        source.foreach(_.close())
      }
    }

    lazy val fileContents: String = read(file)

    checkFunctions.foldLeft(OutputChecker.Result.empty) {
      case (acc, thisCheckFunction) => {

        thisCheckFunction(acc)(
          file.getAbsolutePath)(fileContents)
      }
    }
  }
}

object RegexChecker {
  import OutputChecker.Result.{WarningType, ErrorType}
  type CheckFunction = OutputChecker.Result =>
                       String =>
                       String =>
                       OutputChecker.Result
  type WarningF = (Pattern, String => WarningType) => CheckFunction
  type ErrorF   = (Pattern, String => ErrorType)   => CheckFunction

  trait MkCheckFunctions { this: OutputChecker =>
    /**
     *
     * @param addInType
     * @param shouldMatch this is somewhat confusing: "should" means
     *                    "non-error condition"
     * @param pattern
     * @param mkInType
     * @tparam InType either WarningType or ErrorType
     * @return
     */
    private def mkMatchFunction[InType]
                (addInType: InType =>
                            OutputChecker.Result =>
                            OutputChecker.Result)
                (shouldMatch: Boolean)
                (pattern: Pattern,
                 mkInType: String => InType): CheckFunction =
      start => fileName => fileContents => {

      def error: Boolean = {
        val m = matches(pattern, fileContents)
        if(shouldMatch) {
          m
        } else {
          !m
        }
      }

      if(error) {
        //if the match failed,
        //add our error to the Result
        def expected: String = {
          if(shouldMatch) {
            "match"
          } else {
            "not match"
          }
        }
        val msg = String.format("Error: expected file %s to %s regex %s",
          fileName, expected, pattern.pattern())

        val inType = mkInType(msg)
        addInType(inType)(start)
      } else {
        //otherwise return input
        start
      }
    }

    private def matches(pattern: Pattern, x: String): Boolean =
      pattern.matcher(x).matches()

    private def mkWarningF: Boolean =>
                      (Pattern, String => WarningType) =>
                      CheckFunction = {
      def add: WarningType => OutputChecker.Result => OutputChecker.Result = {
        w => r => r.addWarnings(this, Seq(w))
      }
      mkMatchFunction[WarningType](add)
    }

    private def mkErrorF: Boolean =>
                      (Pattern, String => ErrorType) =>
                      CheckFunction = {

      def add: WarningType => OutputChecker.Result => OutputChecker.Result = {
        e => r => r.addErrors(this, Seq(e))
      }
      mkMatchFunction[ErrorType](add)
    }

    protected def warnIfMatch:    WarningF = mkWarningF(false)
    protected def warnIfNotMatch: WarningF = mkWarningF(true)

    protected def errorIfMatch:    ErrorF = mkErrorF(false)
    protected def errorIfNotMatch: ErrorF = mkErrorF(true)
  }
}