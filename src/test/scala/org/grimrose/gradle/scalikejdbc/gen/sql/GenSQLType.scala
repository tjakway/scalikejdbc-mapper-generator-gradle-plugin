package org.grimrose.gradle.scalikejdbc.gen.sql

case class GenSQLType(
   sqlTypeName: String,
   allowedScalaTypes: Set[String])

object GenSQLType {
  def apply(sqlTypeName: String, allowedScalaType: String): GenSQLType =
    GenSQLType(sqlTypeName, Set(allowedScalaType))

  private object ScalaTypes {
    val intTypes: Set[String] =
      Set("Int", "Long", "BigInteger")

    val floatingTypes: Set[String] =
      Set("Float", "Double", "BigDecimal")

    val textTypes: Set[String] = Set("String")
  }

  object CommonTypes {
    import ScalaTypes._

    case object Decimal extends GenSQLType("DECIMAL", floatingTypes)

    case object Integer extends GenSQLType("INTEGER", intTypes)
    case object Int extends GenSQLType("INT", intTypes)

    case object Text extends GenSQLType("TEXT", textTypes)
    case class VarChar(max: Int) extends GenSQLType(s"VARCHAR($max)", textTypes)
  }
}