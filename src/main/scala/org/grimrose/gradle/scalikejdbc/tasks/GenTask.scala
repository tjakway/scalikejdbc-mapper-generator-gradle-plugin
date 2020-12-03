package org.grimrose.gradle.scalikejdbc.tasks

import java.io.File

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.{Input, Optional, OutputDirectory, TaskAction}
import org.grimrose.gradle.scalikejdbc.ScalikeJDBCMapperGeneratorAdopter

class GenTask extends DefaultTask {

  @OutputDirectory
  var srcDir: File = _

  @OutputDirectory
  var testDir: File = _

  @Input
  var tableName: String = _

  @Input
  @Optional
  var className: String = _

  @TaskAction
  def process(): Unit = {
    val adopter = ScalikeJDBCMapperGeneratorAdopter(getProject)

    val gen = adopter.loadGenerator(getName, getTableName, Option(getClassName), srcDir, testDir)
    gen.foreach { g =>
      g.writeModelIfNonexistentAndUnskippable()
      g.writeSpecIfNotExist(g.specAll())
    }
  }

  def getTableName: String = this.tableName

  def setTableName(tableName: String): Unit = this.tableName = tableName

  def getClassName: String = this.className

  def setClassName(className: String): Unit = this.className = className

  def getSrcDir: File = this.srcDir

  def setSrcDir(srcDir: File): Unit = this.srcDir = srcDir

  def getTestDir: File = this.testDir

  def setTestDir(testDir: File): Unit = this.testDir = testDir
}
