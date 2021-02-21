package org.grimrose.gradle.scalikejdbc.gen.sql

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
