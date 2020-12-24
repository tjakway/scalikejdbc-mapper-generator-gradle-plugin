package org.grimrose.gradle.scalikejdbc

import org.gradle.api.Project
import org.grimrose.gradle.scalikejdbc.ScalikeJDBCMapperGeneratorPlugin.TaskInfo
import org.grimrose.gradle.scalikejdbc.tasks.ScalikejdbcConfigTask
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

trait TestTasks
  extends TestUtil { this: AnyFlatSpec =>
  import Matchers._

  protected def defaultTestSubject: String = "The project"

  protected def mkTaskTest(project: Project, testSubject: String = defaultTestSubject)
                          (taskInfo: TaskInfo): Unit = {
    defaultTestSubject should s"have task ${taskInfo.name}" in {
      val task = project.getTasks.findByName(taskInfo.name)
      task should not(be(null))
      task.getDescription shouldEqual taskInfo.description
      task shouldBe a[ScalikejdbcConfigTask]
    }
  }

  protected def testTasks(project: Project): Unit = {
    applyPlugin(project)
    ScalikeJDBCMapperGeneratorPlugin
      .TaskInfo.all.foreach(mkTaskTest(project))
  }
}
