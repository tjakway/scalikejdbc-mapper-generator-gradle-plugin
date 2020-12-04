package org.grimrose.gradle.scalikejdbc.tasks

import org.gradle.api.tasks.{OutputDirectory, TaskAction}
import org.gradle.api.tasks.{Optional => GradleOptional}
import org.grimrose.gradle.scalikejdbc.ScalikeJDBCMapperGeneratorAdopter
import org.grimrose.gradle.scalikejdbc.ScalikeJDBCMapperGeneratorAdopter.GetGeneratorFor
import org.grimrose.gradle.scalikejdbc.util.Util

import java.io.File
import java.util.{Optional => JOptional}

abstract class GenTask extends ScalikejdbcConfigTask {
  @GradleOptional
  @OutputDirectory
  var srcDir: JOptional[File] = JOptional.empty()

  def setSrcDir(o: JOptional[File]): Unit = {
    srcDir = o
  }
  def setSrcDir(f: File): Unit = setSrcDir(JOptional.of(f))
  def getSrcDir(): JOptional[File] = srcDir

  @GradleOptional
  @OutputDirectory
  var testDir: JOptional[File] = JOptional.empty()

  def setTestDir(o: JOptional[File]): Unit = {
    testDir = o
  }
  def setTestDir(f: File): Unit = setTestDir(JOptional.of(f))
  def getTestDir(): JOptional[File] = testDir

  protected def getGeneratorFor: GetGeneratorFor

  @TaskAction
  def process() = {
    val adopter = ScalikeJDBCMapperGeneratorAdopter(getProject)

    adopter.loadGen(
      this,
      getGeneratorFor,
      Util.asScalaOption(srcDir),
      Util.asScalaOption(testDir)).foreach { g =>
        g.writeModelIfNonexistentAndUnskippable()
        g.writeSpecIfNotExist(g.specAll())
    }
  }
}