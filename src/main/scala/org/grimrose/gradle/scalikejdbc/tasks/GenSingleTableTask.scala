package org.grimrose.gradle.scalikejdbc.tasks

import org.gradle.api.tasks.{Input, Optional}
import org.grimrose.gradle.scalikejdbc.ScalikeJDBCMapperGeneratorAdopter.GetGeneratorFor
import org.grimrose.gradle.scalikejdbc.util.Util

import java.util.{Optional => JOptional}
import scala.beans.BeanProperty

class GenSingleTableTask extends GenTask {
  @Input
  @BeanProperty
  var tableName: String = _

  @Input
  @Optional
  @BeanProperty
  var className: JOptional[String] = JOptional.empty()

  def setClassName(s: String): Unit = JOptional.of(s)
  def setClassName(x: JOptional[String]): Unit = {
    className = x
  }
  def getClassName(): JOptional[String] = className

  override protected def getGeneratorFor: GetGeneratorFor =
    GetGeneratorFor.Table(tableName, Util.asScalaOption(className))
}
