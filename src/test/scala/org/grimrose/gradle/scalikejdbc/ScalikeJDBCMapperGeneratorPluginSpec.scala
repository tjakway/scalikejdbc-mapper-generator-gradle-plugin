package org.grimrose.gradle.scalikejdbc

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.grimrose.gradle.scalikejdbc.ScalikeJDBCMapperGeneratorPlugin.TaskInfo
import org.grimrose.gradle.scalikejdbc.tasks.ScalikejdbcConfigTask
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

@RunWith(classOf[JUnitRunner])
class ScalikeJDBCMapperGeneratorPluginSpec
  extends AnyFlatSpec
    with Matchers
    with WithTempDir
    with TestUtil
    with TestTasks {

  private def testTasks(): Unit = {
    val project = ProjectBuilder.builder().build()

    testTasks(project)
  }

  testTasks()
}
