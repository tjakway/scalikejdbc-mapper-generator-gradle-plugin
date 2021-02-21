package org.grimrose.gradle.scalikejdbc.gen.sql

abstract class ColumnModifier(val sqlRep: String,
                              val modifierPosition:
                                ColumnModifier.ModifierPosition)

object ColumnModifier {
  sealed trait ModifierPosition
  object ModifierPosition {
    case object BeforeIdentifier extends ModifierPosition
    case object AfterIdentifier extends ModifierPosition

    def groupModifiers(xs: Set[ColumnModifier]):
      Map[ModifierPosition, Set[ColumnModifier]] = {

      val zero: Map[ModifierPosition, Set[ColumnModifier]] =
        Map.empty
      xs.foldLeft(zero) {
        case (acc, thisModifier) => {
          val key = thisModifier.modifierPosition
          val currentSet = acc.getOrElse(key, Set.empty)

          val updatedSet = currentSet + thisModifier
          acc.updated(key, updatedSet)
        }
      }
    }
  }

  import ModifierPosition._

  case object PrimaryKey
    extends ColumnModifier("PRIMARY KEY", BeforeIdentifier)

  case object Unique
    extends ColumnModifier("UNIQUE", AfterIdentifier)

  case object NotNull
    extends ColumnModifier("NOT NULL", AfterIdentifier)


  private def joinSingleSpace[A](xs: Set[A]): String = {
    xs.foldLeft("") {
      case (acc, thisX) => acc + " " + thisX.toString
    }
  }
  def printBeforeModifiers(xs: Set[ColumnModifier]): String =
    joinSingleSpace(xs)

  def printAfterModifiers(xs: Set[ColumnModifier]): String =
    joinSingleSpace(xs)

  def unapply(c: ColumnModifier):
    Option[(String, ModifierPosition)] =
      Some((c.sqlRep, c.modifierPosition))
}
