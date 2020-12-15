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
    with WithTempDir {

  val pluginPackage: String = "org.grimrose.gradle.scalikejdbc"

  private def jMap[K, V](xs: Seq[(K, V)]): java.util.Map[K, V] = {
    import scala.collection.JavaConverters
    JavaConverters.mapAsJavaMap(xs.toMap)
  }
  private def jMap[K, V](x: (K, V)): java.util.Map[K, V] = {
    jMap(Seq(x))
  }

  private def mkTaskTest(project: Project)
                        (taskInfo: TaskInfo): Unit = {
    "The project" should s"have task ${taskInfo.name}" in {
      val task = project.getTasks.findByName(taskInfo.name)
      task should not(be(null))
      task.getDescription shouldEqual taskInfo.description
      task shouldBe a[ScalikejdbcConfigTask]
    }
  }

  private def testTasks(): Unit = {
    val project = ProjectBuilder.builder().build()

    project.apply(jMap("plugin" -> pluginPackage))
    ScalikeJDBCMapperGeneratorPlugin
      .TaskInfo.all.foreach(mkTaskTest(project))
  }

  testTasks()
}
