package org.grimrose.gradle.scalikejdbc.tasks

import org.grimrose.gradle.scalikejdbc.ScalikeJDBCMapperGeneratorAdopter.GetGeneratorFor


class GenAllTask extends GenTask {

  override protected def getGeneratorFor: GetGeneratorFor =
    GetGeneratorFor.AllTables
}
