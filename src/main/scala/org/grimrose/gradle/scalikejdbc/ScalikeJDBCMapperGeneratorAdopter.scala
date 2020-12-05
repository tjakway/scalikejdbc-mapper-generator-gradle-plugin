package org.grimrose.gradle.scalikejdbc

import org.gradle.api.Project
import org.grimrose.gradle.scalikejdbc.mapper.ScalikeJDBCMapperGenerator
import org.grimrose.gradle.scalikejdbc.tasks.ScalikejdbcConfigTask
import scalikejdbc.mapper.CodeGenerator

import java.io.File


class ScalikeJDBCMapperGeneratorAdopter(project: Project) {
  import ScalikeJDBCMapperGeneratorAdopter._

  import scala.collection.JavaConverters._

  val generator = new ScalikeJDBCMapperGenerator

  def loadGen(scalikejdbcConfigTask: ScalikejdbcConfigTask,
              getGeneratorFor: GetGeneratorFor,
              srcDir: Option[String],
              testDir: Option[String],
              failOnPermissionError: Boolean = true): Seq[CodeGenerator] = {
    val resolvedSrcDir = getDir(srcDir, getDefaultSrcDir)
    val resolvedTestDir = getDir(testDir, getDefaultTestDir)

    val resolveSettingsF =
      if(scalikejdbcConfigTask.usePropertyFiles.isEmpty) {
        generator.resolveSettingsWithDefaultPaths
      } else {
        generator.resolveSettingsWithPropertyFiles(
          scalikejdbcConfigTask.usePropertyFiles.asScala.toSeq)
      }

    val (jdbcSettings, generatorSettings) =
      resolveSettingsF(
        project,
        scalikejdbcConfigTask.jdbcConfig,
        scalikejdbcConfigTask.generatorConfig,
        ScalikejdbcConfigTask
          .selectLoadPropertiesSetting(scalikejdbcConfigTask)
      )

    val mkGen: ScalikeJDBCMapperGenerator = ScalikeJDBCMapperGenerator(
      scalikejdbcConfigTask.failOnPropertyFilePermissionError)

    import GetGeneratorFor._
    getGeneratorFor match {
      case Table(tableName, optClassName) => {
        mkGen.generator(
          tableName, optClassName,
          resolvedSrcDir, resolvedTestDir,
          jdbcSettings, generatorSettings).toSeq
      }
      case AllTables => {
        mkGen.allGenerators(
          resolvedSrcDir, resolvedTestDir,
          jdbcSettings, generatorSettings)
      }
    }

  }

  private def getDir(in: Option[String], getDefaultLoc: Project => File): File = {
    in.filter(_.trim.isEmpty).map(project.file) match {
      case Some(found) => found
      case None => getDefaultLoc(project)
    }
  }
}

object ScalikeJDBCMapperGeneratorAdopter {
  def apply(project: Project) = new ScalikeJDBCMapperGeneratorAdopter(project)

  //TODO: look up from source sets in project
  def getDefaultSrcDir(project: Project): File =
    project.file("src/main/scala")
  def getDefaultTestDir(project: Project): File =
    project.file("src/test/scala")

  sealed trait GetGeneratorFor
  object GetGeneratorFor {
    case class Table(tableName: String, className: Option[String])
      extends GetGeneratorFor
    case object AllTables extends GetGeneratorFor
  }
}
