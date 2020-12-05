package org.grimrose.gradle.scalikejdbc.tasks

import org.gradle.api.tasks.{OutputDirectory, TaskAction, Optional => GradleOptional}
import org.grimrose.gradle.scalikejdbc.ScalikeJDBCMapperGeneratorAdopter
import org.grimrose.gradle.scalikejdbc.ScalikeJDBCMapperGeneratorAdopter.GetGeneratorFor
import org.grimrose.gradle.scalikejdbc.util.Util
import scalikejdbc.mapper.CodeGenerator

import scala.beans.BeanProperty

abstract class GenTask extends ScalikejdbcConfigTask {
  @GradleOptional
  @BeanProperty
  @OutputDirectory
  var srcDir: String = _

  @GradleOptional
  @BeanProperty
  @OutputDirectory
  var testDir: String = _

  protected def getGeneratorFor: GetGeneratorFor

  protected def handleCodeGenerator(g: CodeGenerator): Unit

  @TaskAction
  def process() = {
    val adopter = ScalikeJDBCMapperGeneratorAdopter(getProject)

    adopter.loadGen(
      this,
      getGeneratorFor,
      Util.optionalString(srcDir),
      Util.optionalString(testDir)).foreach { g =>
      handleCodeGenerator(g)
    }
  }
}

object GenTask {
  trait WriteTables {
    protected def forceWrite: Boolean = false

    protected def handleCodeGenerator(g: CodeGenerator): Unit = {
      lazy val specs = g.specAll()
      if(forceWrite) {
        g.writeModel()
        g.writeSpec(specs)
      } else {
        g.writeModelIfNonexistentAndUnskippable()
        g.writeSpecIfNotExist(specs)
      }
    }
  }

  trait ForceWriteTables extends WriteTables {
    override protected def forceWrite: Boolean = true
  }

  trait PrintTables {
    protected def printF: String => Unit = println
    protected def handleCodeGenerator(g: CodeGenerator): Unit = {
      printF(g.modelAll())
      g.specAll().foreach(spec => printF(spec))
    }
  }
}