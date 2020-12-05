package org.grimrose.gradle.scalikejdbc.tasks

import org.gradle.api.tasks.options.{Option => GradleOption}
import org.gradle.api.tasks.{Input, Optional}
import org.grimrose.gradle.scalikejdbc.ScalikeJDBCMapperGeneratorAdopter.GetGeneratorFor
import org.grimrose.gradle.scalikejdbc.ScalikeJDBCMapperGeneratorPlugin
import org.grimrose.gradle.scalikejdbc.util.Util

import scala.beans.BeanProperty

abstract class GenSingleTableTask
  extends GenTask {
  @Input
  @GradleOption(
    option = ScalikeJDBCMapperGeneratorPlugin.Keys.tableName,
    description = "table to generate code for")
  @BeanProperty
  var tableName: String = _

  @Input
  @GradleOption(
    option = ScalikeJDBCMapperGeneratorPlugin.Keys.className,
    description = "name of the generated class (defaults to table name)")
  @Optional
  @BeanProperty
  var className: String = _

  override protected def getGeneratorFor: GetGeneratorFor =
    GetGeneratorFor.Table(tableName, Util.optionalString(className))
}
