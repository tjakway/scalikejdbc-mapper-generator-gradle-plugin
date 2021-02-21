package org.grimrose.gradle.scalikejdbc.gen.sql

class SQLType(
   val sqlTypeName: String,
   val allowedScalaTypes: Set[String])

object SQLType {
  def apply(sqlTypeName: String, allowedScalaType: String): SQLType =
    new SQLType(sqlTypeName, Set(allowedScalaType))

  private object ScalaTypes {
    val intTypes: Set[String] =
      Set("Int", "Long", "BigInteger")

    val floatingTypes: Set[String] =
      Set("Float", "Double", "BigDecimal")

    val textTypes: Set[String] = Set("String")
  }

  object CommonTypes {
    import ScalaTypes._

    case object Decimal extends SQLType("DECIMAL", floatingTypes)

    case object Integer extends SQLType("INTEGER", intTypes)
    case object Int extends SQLType("INT", intTypes)

    case object Text extends SQLType("TEXT", textTypes)
    case class VarChar(max: Int) extends SQLType(s"VARCHAR($max)", textTypes)
  }
}