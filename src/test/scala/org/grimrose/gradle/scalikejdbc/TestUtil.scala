package org.grimrose.gradle.scalikejdbc

import org.gradle.api.Project

trait TestUtil extends TestDefs {
  def jMap[K, V](xs: Seq[(K, V)]): java.util.Map[K, V] = {
    import scala.collection.JavaConverters
    JavaConverters.mapAsJavaMap(xs.toMap)
  }

  def jMap[K, V](x: (K, V)): java.util.Map[K, V] = {
    jMap(Seq(x))
  }

  def applyPlugin(toProject: Project): Unit = {
    toProject.apply(jMap("plugin" -> pluginPackageName))
  }
}
