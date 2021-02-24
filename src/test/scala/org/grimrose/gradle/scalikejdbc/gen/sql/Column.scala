package org.grimrose.gradle.scalikejdbc.gen.sql

import org.grimrose.gradle.scalikejdbc.gen.GenCommon
import org.scalacheck.Gen

case class Column(name: String,
                  sqlType: SQLType,
                  modifiers: Set[ColumnModifier]) {

  private lazy val groupedModifiers:
    Map[ColumnModifier.ModifierPosition, Set[ColumnModifier]] =
    ColumnModifier.ModifierPosition.groupModifiers(modifiers)

  private lazy val beforeModifiers: Set[ColumnModifier] = {
    groupedModifiers.getOrElse(
      ColumnModifier.ModifierPosition.BeforeIdentifier, Set.empty)
  }

  private lazy val afterModifiers: Set[ColumnModifier] = {
    groupedModifiers.getOrElse(
      ColumnModifier.ModifierPosition.AfterIdentifier, Set.empty)
  }

  def print: String = {
    String.format(
      "%s %s %s %s",
      ColumnModifier.printBeforeModifiers(beforeModifiers),
      name,
      sqlType.sqlTypeName,
      ColumnModifier.printAfterModifiers(afterModifiers)
      //trim will remove leading or trailing spaces resulting from
      //prepending and appending empty sets of modifiers
    ).trim
  }
}

object Column {
  def idColumn(driver: SQLDriver): Column = Column(
    "id",
    SQLType.CommonTypes.Integer,
    Set(ColumnModifier.PrimaryKey, driver.getAutoincrementModifier)
  )

  def gen(driver: SQLDriver): Gen[Column] = {
    for {
      name <- GenCommon.genIdentifier
      sqlType <- Gen.oneOf(driver.sqlTypes.toSeq)
      modifiers <- ColumnModifier.gen(driver)
    } yield {
      Column(name, sqlType, modifiers)
    }
  }

  def genColumns(driver: SQLDriver): Gen[Seq[Column]] = {
    Gen.option(idColumn(driver)).flatMap { optIdColumn =>
      Gen.listOf(gen(driver)).flatMap { columns =>
        optIdColumn match {
          case Some(id) => {
            val allColumns = id :: columns
            GenCommon.shuffle(allColumns)
          }
          case None => Gen.const(columns)
        }
      }
    }
  }
}