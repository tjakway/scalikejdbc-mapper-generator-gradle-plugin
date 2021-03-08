package org.grimrose.gradle.scalikejdbc.gen.sql.checks

import org.grimrose.gradle.scalikejdbc.gen.sql.GenSQLException

import java.io.File

trait OutputChecker {
  def apply(buildDir: File): OutputChecker.OutputCheckerResult
}

object OutputChecker {
  import OutputCheckerResult.{WarningType, ErrorType}
  case class OutputCheckerResult(
      errors: Map[OutputChecker, Seq[ErrorType]],
      warnings: Map[OutputChecker, Seq[WarningType]]) {

    //could refactor this to take getters & setters as parameters
    //to avoid duplicating code between addWarnings and addErrors,
    //but the added complexity is not worth it for 2 small methods
    def addWarnings(checker: OutputChecker,
                    toAdd: Seq[WarningType]): OutputCheckerResult = {
      val existing = warnings.getOrElse(checker, Seq.empty)
      val newWarnings = warnings.updated(checker, existing ++ toAdd)
      copy(warnings = newWarnings)
    }

    def addErrors(checker: OutputChecker,
                  toAdd: Seq[ErrorType]): OutputCheckerResult = {
      val existing = errors.getOrElse(checker, Seq.empty)
      val newErrors = errors.updated(checker, existing ++ toAdd)
      copy(errors = newErrors)
    }

    def noWarnings: Boolean = warnings.isEmpty
    def noErrors: Boolean = errors.isEmpty

    def combine(other: OutputCheckerResult): OutputCheckerResult = {
      OutputCheckerResult.combine(this, other)
    }
  }

  object OutputCheckerResult {
    type ErrorType = GenSQLException
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

    def combine(left: OutputCheckerResult,
                right: OutputCheckerResult): OutputCheckerResult = {

      OutputCheckerResult(
        errors = mergeMaps(left.errors, right.errors),
        warnings = mergeMaps(left.warnings, right.warnings)
      )
    }
  }
}