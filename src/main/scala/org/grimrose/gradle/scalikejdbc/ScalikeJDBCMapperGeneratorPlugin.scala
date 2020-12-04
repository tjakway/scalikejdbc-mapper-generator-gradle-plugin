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
    mk[GenSingleTableTask](TaskNames.genSingleTableTask) { task =>
      task.setDescription("Generates a model for a specified table")
    }

    // scalikejdbc-gen-force
    mk[GenForceTask](TaskNames.genAllForceTask) { task =>
      task.setDescription("Generates and overwrites " +
        "a model for a specified table")
    }

    // scalikejdbc-gen-all
    mk[GenAllTask](TaskNames.genAllTask) { task =>
      task.setDescription("Generates models for all tables")
    }

    // scalikejdbc-gen-all-force
    mk[GenAllForceTask](TaskNames.genAllForceTask) { task =>
      task.setDescription("Generates and overwrites models for all tables")
    }

    // scalikejdbc-gen-echo
    mk[GenEchoTask](TaskNames.genEchoTask) { task =>
      task.setDescription("Prints a model for a specified table")
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

    val tableName: String = "tableName"
    val className: String = "className"

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
