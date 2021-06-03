package org.grimrose.gradle.scalikejdbc.gen.sql.checks

import org.grimrose.gradle.scalikejdbc.gen.sql.GenSQLException

import java.io.File

class CombinedChecker(val checkers: Set[OutputChecker])
  extends OutputChecker {
  import CombinedChecker._
  override def apply(buildDir: File): OutputChecker.Result = {
    ??? //TODO
  }

  private def perScalaFile(f: File): OutputChecker.Result = {
    checkers.foldLeft(Map.empty: ResultMap) {
      case (acc, thisChecker) => {
        ??? //TODO
      }
    }
    ??? //TODO
  }
}

object CombinedChecker {
  private type ResultMap = Map[OutputChecker, OutputChecker.Result]

  /**
   * TODO: implement fail-on-warning (probably pass an instance of test config)
   * @param passed
   * @param failed
   */
  private case class SortedResults(passed: ResultMap,
                                   failed: ResultMap) {

    private def updateMap(key: OutputChecker,
                          res: OutputChecker.Result,
                          target: String)
                         (x: ResultMap): ResultMap = {
      x.get(key) match {
        case Some(existingRes) =>
          throw DuplicateCheckError(key, res, existingRes, target)

        case None => x.updated(key, res)
      }
    }

    private def passed(res: OutputChecker.Result): Boolean =
      res.noErrors

    def update(thisChecker: OutputChecker,
               thisRes: OutputChecker.Result,
               target: String): SortedResults = {
      def updateMapF = updateMap(thisChecker, thisRes, target) _

      if(passed(thisRes)) {
        copy(passed = updateMapF(passed))
      } else {
        copy(failed = updateMapF(failed))
      }
    }

    private def checkNotEmpty(): Unit = {
      if(passed.isEmpty && failed.isEmpty) {
        throw NoCheckersError
      } else {}
    }
  }

  class CombinedCheckerError(override val msg: String)
    extends GenSQLException(msg)

  /**
   * This is an exception instead of an error lodged in OutputChecker.Result
   * because it indicates the orchestrating test code is the problem,
   * not the generated SQL or scalikejdbc
   * @param msg
   */
  case class DuplicateCheckError(override val msg: String)
    extends CombinedCheckerError(msg)

  object DuplicateCheckError {
    def apply(checker: OutputChecker,
              newRes: OutputChecker.Result,
              prevRes: OutputChecker.Result,
              target: String): DuplicateCheckError = {
      DuplicateCheckError(s"Error running $checker on $target: " +
        s"got $newRes but $prevRes already exists in ResultMap")
    }
  }

  case object NoCheckersError extends CombinedCheckerError(
    "Expected to run at least one OutputChecker")
}