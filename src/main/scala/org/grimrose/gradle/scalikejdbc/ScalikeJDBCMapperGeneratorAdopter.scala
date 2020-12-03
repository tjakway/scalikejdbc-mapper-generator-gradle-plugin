package org.grimrose.gradle.scalikejdbc

import org.gradle.api.{InvalidUserDataException, Project}
import org.grimrose.gradle.scalikejdbc.mapper.ScalikeJDBCMapperGenerator
import org.grimrose.gradle.scalikejdbc.tasks.ScalikejdbcConfigTask

import java.io.File
import scala.util.control.Exception._


class ScalikeJDBCMapperGeneratorAdopter(project: Project) {
  import ScalikeJDBCMapperGeneratorAdopter._

  import scala.collection.JavaConverters._

  val generator = new ScalikeJDBCMapperGenerator

  def loadSettings() = {
    val path = targetOrDefaultDirectory("project", "project").getAbsolutePath
    generator.loadSettings(path)
  }

  def loadGenerator(taskName: String, tableName: String, className: Option[String], srcDir: AnyRef, testDir: AnyRef) = {
    if (Option(tableName).getOrElse("").isEmpty) {
      val log = project.getLogger
      log.error(s"Not a valid command: $taskName")
      log.error(s"Usage: $taskName -PtableName=table_name (-PclassName=class_name)")
      throw new InvalidUserDataException(s"$taskName: tableName is empty.")
    }

    val s = targetOrDefaultDirectory(srcDir, "src/main/scala")
    val t = targetOrDefaultDirectory(testDir, "src/test/scala")

    generator.generator(tableName, className, s, t, loadSettings._1, loadSettings._2)
  }

  def allGenerators(srcDir: AnyRef, testDir: AnyRef) = {
    val s = targetOrDefaultDirectory(srcDir, "src/main/scala")
    val t = targetOrDefaultDirectory(testDir, "src/test/scala")

    generator.allGenerators(s, t, loadSettings._1, loadSettings._2)
  }

  def targetOrDefaultDirectory(target: AnyRef, defaultPath: String) = allCatch.opt(project.file(target)).getOrElse(project.file(defaultPath))


  def loadGen(scalikejdbcConfigTask: ScalikejdbcConfigTask,
              srcDir: Option[AnyRef],
              testDir: Option[AnyRef]) = {
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


  }

  private def getDir(in: Option[AnyRef], getDefaultLoc: Project => File): File = {
    in.map(_.toString).filter(_.trim.isEmpty).map(project.file) match {
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
}
