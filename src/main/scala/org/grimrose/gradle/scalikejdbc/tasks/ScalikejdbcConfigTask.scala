package org.grimrose.gradle.scalikejdbc.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.{Input, Nested}
import org.grimrose.gradle.scalikejdbc.interop
import org.grimrose.gradle.scalikejdbc.interop.GeneratorConfig
import org.grimrose.gradle.scalikejdbc.mapper.ScalikeJDBCMapperGenerator

import java.io.File
import java.util.{ArrayList, Collection}
import scala.beans.BeanProperty



class ScalikejdbcConfigTask extends DefaultTask {
  @Input
  @BeanProperty
  @Nested
  var jdbcConfig: interop.JdbcConfig = new interop.JdbcConfig()

  @Input
  @BeanProperty
  @Nested
  var generatorConfig: interop.GeneratorConfig = new GeneratorConfig()


  @Input
  @BeanProperty
  var usePropertyFiles: Collection[File] = new ArrayList()

  /**
   * whether settings defined in this gradle task can silently override
   * settings defined in mapper .properties files
   * set to false to throw an exception instead
   */
  @Input
  @BeanProperty
  var allowOverridingPropertyFiles: Boolean = true

  @Input
  @BeanProperty
  var failOnPropertyFilePermissionError: Boolean = true
}

object ScalikejdbcConfigTask {
  import ScalikeJDBCMapperGenerator.LoadPropertiesSetting
  def selectLoadPropertiesSetting(in: ScalikejdbcConfigTask):
    LoadPropertiesSetting = {

    if(in.allowOverridingPropertyFiles) {
      LoadPropertiesSetting.PreferGradle
    } else {
      LoadPropertiesSetting.AssertNoConflict
    }
  }
}
