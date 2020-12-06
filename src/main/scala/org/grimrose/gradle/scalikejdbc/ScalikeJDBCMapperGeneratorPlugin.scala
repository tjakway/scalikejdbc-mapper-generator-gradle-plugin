package org.grimrose.gradle.scalikejdbc

import org.gradle.api.{Plugin, Project, Task}
import org.grimrose.gradle.scalikejdbc.tasks._

import scala.collection.JavaConverters._
import scala.language.existentials

class ScalikeJDBCMapperGeneratorPlugin extends Plugin[Project] {
  import ScalikeJDBCMapperGeneratorPlugin._

  //add tasks to the project
  override def apply(project: Project): Unit = {
    val makeTask: MakeTask = new MakeTask(project)

    // scalikejdbc-gen
    makeTask[GenSingleTableWriteTask](TaskInfo.genSingleTableTask)

    // scalikejdbc-gen-force
    makeTask[GenForceTask](TaskInfo.genSingleTableForceTask)

    // scalikejdbc-gen-all
    makeTask[GenAllWriteTask](TaskInfo.genAllTask)

    // scalikejdbc-gen-all-force
    makeTask[GenAllForceTask](TaskInfo.genAllForceTask)

    // scalikejdbc-gen-echo
    makeTask[GenSingleTableEchoTask](TaskInfo.genEchoTask)

    // new task not present in scalikejdbc: scalikejdbcGenEchoAll
    // trivial to implement and convenient
    makeTask[GenAllEchoTask](TaskInfo.genEchoAllTask)
  }

  private class MakeTask(val project: Project) {
    def apply[T <: Task](taskInfo: TaskInfo)
                     (implicit m: Manifest[T]): T = {

      def configure: (T) => Unit = { task =>
        task.setDescription(taskInfo.description)
      }

      makeTask[T](project, taskInfo.name)(configure)(m)
    }

    private def makeTask[T <: Task](p: Project, name: String)
                           (configure: (T) => Unit)
                           (implicit m: Manifest[T]): T = {
      val map = Map("type" -> m.runtimeClass)
      val t = p.task(map.asJava, name).asInstanceOf[T]
      t.setGroup(ScalikeJDBCMapperGeneratorPlugin.Keys.taskGroup)
      configure(t)
      t
    }
  }
}

object ScalikeJDBCMapperGeneratorPlugin {
  object Keys {
    val taskGroup: String = "ScalikeJDBC Mapper Generator"

    final val tableName = "tableName"
    final val className = "className"
  }

  case class TaskInfo(name: String, description: String)
  object TaskInfo {
    private val prefix: String = "scalikejdbc"

    val genSingleTableTask: TaskInfo =
      TaskInfo(prefix + "Gen", "Generates a model for a specified table")

    val genSingleTableForceTask: TaskInfo =
      TaskInfo(genSingleTableTask + "Force", "Generates and overwrites " +
        "a model for a specified table")

    val genAllTask: TaskInfo =
      TaskInfo(prefix + "GenAll", "Generates models for all tables")

    val genAllForceTask: TaskInfo =
      TaskInfo(genAllTask + "Force",
        "Generates and overwrites models for all tables")

    val genEchoTask: TaskInfo =
      TaskInfo(prefix + "GenEcho", "Prints a model for a specified table")

    val genEchoAllTask: TaskInfo =
      TaskInfo(genEchoTask + "All", "Prints models for all tables")

    val all: Set[TaskInfo] = Set(
      genSingleTableTask,
      genSingleTableForceTask,
      genAllTask,
      genAllForceTask,
      genEchoTask,
      genEchoAllTask)
  }
}
