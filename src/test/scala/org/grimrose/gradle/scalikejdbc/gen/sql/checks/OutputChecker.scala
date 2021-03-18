package org.grimrose.gradle.scalikejdbc.gen.sql.checks

import org.grimrose.gradle.scalikejdbc.gen.sql.GenSQLException

import java.io.File
import java.util.Formatter

trait OutputChecker {
  def apply(buildDir: File): OutputChecker.Result
}

object OutputChecker {
  import Result.{WarningType, ErrorType}
  case class Result(
      header: Option[String],
      errors: Map[OutputChecker, Seq[ErrorType]],
      warnings: Map[OutputChecker, Seq[WarningType]]) {

    //could refactor this to take getters & setters as parameters
    //to avoid duplicating code between addWarnings and addErrors,
    //but the added complexity is not worth it for 2 small methods
    def addWarnings(checker: OutputChecker,
                    toAdd: Seq[WarningType]): Result = {
      val existing = warnings.getOrElse(checker, Seq.empty)
      val newWarnings = warnings.updated(checker, existing ++ toAdd)
      copy(warnings = newWarnings)
    }

    def addErrors(checker: OutputChecker,
                  toAdd: Seq[ErrorType]): Result = {
      val existing = errors.getOrElse(checker, Seq.empty)
      val newErrors = errors.updated(checker, existing ++ toAdd)
      copy(errors = newErrors)
    }

    def noWarnings: Boolean = warnings.isEmpty
    def noErrors: Boolean = errors.isEmpty

    def combine(other: Result): Result = {
      Result.combine(this, other)
    }
  }

  object Result {
    type ErrorType = Throwable
    type WarningType = Throwable

    private def mergeMaps[K, E](left: Map[K, Seq[E]],
                                right: Map[K, Seq[E]]):
      Map[K, Seq[E]] = {

      right.foldLeft(left) {
        case (acc, (thisKey, values)) => {
          val currentValues = acc.getOrElse(thisKey, Seq.empty)
          val newValues = currentValues ++ values
          acc.updated(thisKey, newValues)
        }
      }
    }

    private def withFormatter(f: Formatter => Unit): String = {
      val fmt: Formatter = new Formatter(new StringBuffer())
      f(fmt)
      fmt.toString.trim
    }

    private def printSeq[A](xs: Seq[A],
                            indentation: String): String = {

      withFormatter { fmt =>
        xs.foreach(x => fmt.format("%s%s\n", indentation, x.toString))
      }
    }

    private def defaultHeader(r: Result): String =
      r.getClass.getName

    private val subsectionIndentation: String = "\t"

    def print(r: Result): String = {

      withFormatter { fmt =>
        fmt.format("%s\n%s\n%s\n",
          defaultHeader(r),
          printSeq(r.errors, subsectionIndentation)
          )
      }
    }

    def combine(left: Result,
                right: Result): Result = {
      Result(
        mergeHeaders(left.header, right.header),
        errors = mergeMaps(left.errors, right.errors),
        warnings = mergeMaps(left.warnings, right.warnings)
      )
    }

    private def mergeHeaders(left: Option[String],
                             right: Option[String]): Option[String] = {

      (left, right) match {
        case (None, None) => None
        case _ => {
          Some(s"Headers < " + left.getOrElse("None") + ", " +
            right.getOrElse(None) + " >")
        }
      }
    }
  }
}