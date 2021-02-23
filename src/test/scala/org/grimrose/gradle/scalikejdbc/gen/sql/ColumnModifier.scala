package org.grimrose.gradle.scalikejdbc.gen.sql

import org.scalacheck.Gen

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

  def isPrimaryKey(columnModifier: ColumnModifier): Boolean =
    columnModifier == PrimaryKey

  case object Unique
    extends ColumnModifier("UNIQUE", AfterIdentifier)

  case object NotNull
    extends ColumnModifier("NOT NULL", AfterIdentifier)

  /**
   * Note: contains mutually exclusive modifiers
   * (i.e. you cannot construct a Column with every modifier)
   * @return
   */
  def allModifiers: Set[ColumnModifier] = Set(
  )

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


  def filterPrimaryKeys(xs: Iterable[ColumnModifier]): Set[ColumnModifier] = {
    val empty: (Set[ColumnModifier], Boolean) = (Set.empty, false)

    val res = xs.foldLeft(empty) {
      case ((acc, hasPrimaryKey), thisModifier) => {
        if(isPrimaryKey(thisModifier)) {
          if(hasPrimaryKey) {
            (acc, true)
          } else {
            (acc + thisModifier, true)
          }
        } else {
          (acc + thisModifier, hasPrimaryKey)
        }
      }
    }
    res._1
  }

  //TODO: need to integrate with SQLDriver
  def gen(driver: SQLDriver): Gen[Set[ColumnModifier]] = {
    val genUniqueness: Gen[Option[ColumnModifier]] = {
      Gen.option(Gen.oneOf(Unique, NotNull))
    }

    val genPrimaryKey: Gen[Option[ColumnModifier]] = Gen.option(PrimaryKey)

    //TODO: add others
    val otherModifiers: Gen[Set[ColumnModifier]] = Gen.const(Set.empty)

    for {
      uniq <- genUniqueness
      pk <- genPrimaryKey
      rest <- otherModifiers
    } yield {
      uniq.toSet ++ pk.toSet ++ rest
    }
  }
}
