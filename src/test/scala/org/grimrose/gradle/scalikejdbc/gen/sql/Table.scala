package org.grimrose.gradle.scalikejdbc.gen.sql

import org.grimrose.gradle.scalikejdbc.gen.GenCommon
import org.scalacheck.Gen

import java.util.regex.Pattern
import scala.util.{Failure, Success, Try}

case class Table(name: String,
                 columns: Seq[Column]) {
  private def printColumns(columnSeparator: String): String = {
    if(columns.isEmpty) {
      ""
    } else {
      columns.map(_.print).reduce(_ + columnSeparator + _)
    }
  }

  private def printColumnSection(columnSeparator: String): String = {
    if(columns.isEmpty) {
      ""
    } else {
      "(" + printColumns(columnSeparator) + ")"
    }
  }

  def getCreateTableStatement(columnSeparator: String =
                              Table.defaultColumnSeparator): String = {
    String.format("CREATE TABLE %s%s",
      name, printColumnSection(columnSeparator))
  }
}

object Table {
  def defaultColumnSeparator: String = ", "

  class GenTableException(override val msg: String)
    extends GenSQLException(msg)


  def gen(driver: SQLDriver): Gen[Table] = {
    for {
      name <- GenCommon.genIdentifier
      columns <- Column.genColumns(driver)
    } yield {
      Table(name, columns)
    }
  }

  object CheckCreateTableStatement {
    private val checkRegexStr: String = """"""
    private lazy val checkRegex: Either[Throwable, Pattern] = {
      Try(Pattern.compile(checkRegexStr, Pattern.MULTILINE)) match {
        case Success(r) => Right(r)
        case Failure(t) => {
          val ex = new GenTableException(
            s"Failed to compile check table regex < $checkRegexStr >")
          ex.initCause(t)
          Left(ex)
        }
      }
    }

    private def checkMatches(in: String): Either[Throwable, Unit] = {
      checkRegex.flatMap { r =>
        val matcher = r.matcher(in)
        if(matcher.matches()) {
          Right({})
        } else {
          Left(new GenTableException(s"Expected generated " +
            s"create table statement < $in > to match validation regex " +
            s"< $checkRegexStr >"))
        }
      }
    }


    def apply: String => Either[Throwable, Unit] = checkMatches
  }
}