package org.grimrose.gradle.scalikejdbc

import org.gradle.api.{Plugin, Project, Task}
import org.grimrose.gradle.scalikejdbc.tasks._

import scala.collection.JavaConverters._
import scala.language.existentials

class ScalikeJDBCMapperGeneratorPlugin extends Plugin[Project] {
  import ScalikeJDBCMapperGeneratorPlugin.Keys._

  //add tasks to the project
  override def apply(project: Project): Unit = {
    def mk[T <: Task](name: String)
                     (configure: (T) => Unit)
                     (implicit m: Manifest[T]): T = {
      makeTask[T](project, name)(configure)(m)
    }

    // scalikejdbc-gen
    mk[GenSingleTableWriteTask](TaskNames.genSingleTableTask) { task =>
      task.setDescription("Generates a model for a specified table")
    }

    // scalikejdbc-gen-force
    mk[GenForceTask](TaskNames.genAllForceTask) { task =>
      task.setDescription("Generates and overwrites " +
        "a model for a specified table")
    }

    // scalikejdbc-gen-all
    mk[GenAllWriteTask](TaskNames.genAllTask) { task =>
      task.setDescription("Generates models for all tables")
    }

    // scalikejdbc-gen-all-force
    mk[GenAllForceTask](TaskNames.genAllForceTask) { task =>
      task.setDescription("Generates and overwrites models for all tables")
    }

    // scalikejdbc-gen-echo
    mk[GenSingleTableEchoTask](TaskNames.genEchoTask) { task =>
      task.setDescription("Prints a model for a specified table")
    }

    mk[GenAllEchoTask](TaskNames.genEchoAllTask) { task =>
      task.setDescription("Prints models for all tables")
    }
  }

  def makeTask[T <: Task](p: Project, name: String)
                         (configure: (T) => Unit)
                         (implicit m: Manifest[T]): T = {
    val map = Map("type" -> m.runtimeClass)
    val t = p.task(map.asJava, name).asInstanceOf[T]
    t.setGroup(ScalikeJDBCMapperGeneratorPlugin.Keys.taskGroup)
    configure(t)
    t
  }
}

object ScalikeJDBCMapperGeneratorPlugin {
  object Keys {
    val taskGroup: String = "ScalikeJDBC Mapper Generator"

    final val tableName = "tableName"
    final val className = "className"

    object TaskNames {
      val prefix: String = "scalikejdbc"
      val genSingleTableTask: String = prefix + "Gen"
      val genSingleTableForceTask: String = genSingleTableTask + "Force"
      val genAllTask: String = prefix + "GenAll"
      val genAllForceTask: String = genAllTask + "Force"
      val genEchoTask: String = prefix + "GenEcho"
      val genEchoAllTask: String = genEchoTask + "All"
    }
  }
}
