/*
 * Copyright 2012 Kazuhiro Sera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 *
 * ***********************************************************
 *
 * Adapted from scalikejdbc/scalikejdbc-mapper-generator/src/main/scala/scalikejdbc/mapper/ScalikejdbcPlugin.scala,
 * commit fe07ea49b7ab0e32eb9abeb3abaab5b2baaa1a24 on 2020-11-27 by Thomas Jakway
 */
package org.grimrose.gradle.scalikejdbc.mapper

import java.io.{File, FileNotFoundException}
import java.util.Locale.{ENGLISH => en}
import java.util.Properties
import java.util.regex.Pattern

import org.grimrose.gradle.scalikejdbc.MapperException
import org.grimrose.gradle.scalikejdbc.util.Util
import org.slf4j.{Logger, LoggerFactory}
import scalikejdbc.mapper.{CodeGenerator, DateTimeClass, GeneratorConfig, GeneratorTemplate, GeneratorTestTemplate, LineBreak, Model, ReturnCollectionType, Table}

import scala.collection.JavaConverters
import scala.language.reflectiveCalls
import scala.util.control.Exception._

/**
 * ScalikeJDBC Mapper Generator
 *
 * @see scalikejdbc.mapper.SbtPlugin
 */
class ScalikeJDBCMapperGenerator {
  import ScalikeJDBCMapperGenerator._
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  //TODO: get the file using project.file()
  def loadSettings(projectDirectoryPath: String): (JDBCSettings, GeneratorSettings) = {
    val props = new Properties()
    try {
      val file: File = new File(projectDirectoryPath, "scalikejdbc-mapper-generator.properties")
      println(file.getAbsolutePath)
      using(new java.io.FileInputStream(file)) {
        inputStream => props.load(inputStream)
      }
    } catch {
      case e: FileNotFoundException =>
    }
    if (props.isEmpty) {
      val file: File = new File(projectDirectoryPath, "scalikejdbc.properties")
      println(file.getAbsolutePath)
      using(new java.io.FileInputStream(file)) {
        inputStream => props.load(inputStream)
      }
    }

    CheckKey.checkKeys(props)

    val defaultConfig = GeneratorConfig()
    def getString = ScalikeJDBCMapperGenerator.getString(props) _

    import Keys._
    lazy val generatorSettings: GeneratorSettings = GeneratorSettings(
      packageName = getString(PACKAGE_NAME).getOrElse(defaultConfig.packageName),
      template = getString(TEMPLATE).getOrElse(defaultConfig.template.name),
      testTemplate = getString(TEST_TEMPLATE).getOrElse(GeneratorTestTemplate.specs2unit.name),
      lineBreak = getString(LINE_BREAK).getOrElse(defaultConfig.lineBreak.name),
      encoding = getString(ENCODING).getOrElse(defaultConfig.encoding),
      autoConstruct = getString(AUTO_CONSTRUCT).map(_.toBoolean).getOrElse(defaultConfig.autoConstruct),
      defaultAutoSession = getString(DEFAULT_AUTO_SESSION).map(_.toBoolean).getOrElse(defaultConfig.defaultAutoSession),
      dateTimeClass = getString(DATETIME_CLASS).map {
        name => dateTimeClassMap.getOrElse(name, sys.error("does not support " + name))
      }.getOrElse(defaultConfig.dateTimeClass),
      defaultConfig.tableNameToClassName,
      defaultConfig.columnNameToFieldName,
      returnCollectionType = getString(RETURN_COLLECTION_TYPE).map { name =>
        returnCollectionTypeMap.getOrElse(
          name.toLowerCase(en),
          sys.error(s"does not support $name. " +
            s"Supported types are ${returnCollectionTypeMap.keys.mkString(", ")}"))
      }.getOrElse(defaultConfig.returnCollectionType),
      view = getString(VIEW).map(_.toBoolean).getOrElse(defaultConfig.view),
      tableNamesToSkip = getString(TABLE_NAMES_TO_SKIP).map(_.split(",").toList).getOrElse(defaultConfig.tableNamesToSkip),
      baseTypes = commaSeparated(props, BASE_TYPES),
      companionBaseTypes = commaSeparated(props, COMPANION_BASE_TYPES),
      tableNameToSyntaxName = defaultConfig.tableNameToSyntaxName,
      tableNameToSyntaxVariableName = defaultConfig.tableNameToSyntaxVariableName)

    lazy val jdbcSettings = JDBCSettings(
        driver = getString(JDBC_DRIVER)
          .getOrElse(throw new MapperGeneratorConfigException(s"Add $JDBC_DRIVER to project/scalikejdbc-mapper-generator.properties")),
        url = getString(JDBC_URL)
          .getOrElse(throw new MapperGeneratorConfigException(s"Add $JDBC_URL to project/scalikejdbc-mapper-generator.properties")),
        username = getString(JDBC_USER_NAME).getOrElse(""),
        password = getString(JDBC_PASSWORD).getOrElse(""),
        schema = getString(JDBC_SCHEMA).orNull[String])

    (jdbcSettings, generatorSettings)
  }

  def generatorConfig(srcDir: File, testDir: File, generatorSettings: GeneratorSettings): GeneratorConfig =
    GeneratorConfig(
      srcDir = srcDir.getAbsolutePath,
      testDir = testDir.getAbsolutePath,
      packageName = generatorSettings.packageName,
      template = GeneratorTemplate(generatorSettings.template),
      testTemplate = GeneratorTestTemplate(generatorSettings.testTemplate),
      lineBreak = LineBreak(generatorSettings.lineBreak),
      encoding = generatorSettings.encoding,
      autoConstruct = generatorSettings.autoConstruct,
      defaultAutoSession = generatorSettings.defaultAutoSession,
      dateTimeClass = generatorSettings.dateTimeClass,
      tableNameToClassName = generatorSettings.tableNameToClassName,
      columnNameToFieldName = generatorSettings.columnNameToFieldName,
      returnCollectionType = generatorSettings.returnCollectionType,
      view = generatorSettings.view,
      tableNamesToSkip = generatorSettings.tableNamesToSkip,
      tableNameToBaseTypes = _ => generatorSettings.baseTypes,
      tableNameToCompanionBaseTypes = _ => generatorSettings.companionBaseTypes,
      tableNameToSyntaxName = generatorSettings.tableNameToSyntaxName,
      tableNameToSyntaxVariableName = generatorSettings.tableNameToSyntaxVariableName)

  def generator(tableName: String, className: Option[String],
                srcDir: File, testDir: File,
                jdbc: JDBCSettings,
                generatorSettings: GeneratorSettings): Option[CodeGenerator] = {
    def errMessage(model: Model,
                   printAllTables: Boolean): String = {
      val classNameStr =
        className.map(name => s" (used to generate class $name)")
      val fmtStr = "Could not find table %s" +
        classNameStr.getOrElse("") +
        "\n\tsrcDir: %s" +
        "\n\ttestDir: %s" +
        "\n\tJDBCSettings: %s" +
        "\n\tGeneratorSettings: %s" +
        "%s" //allTablesStr

      def allTablesStr: String = {
        if(printAllTables) {
          val foundTables = allTables(true, jdbc, model)
          val foundTablesStr = {
            if(foundTables.isEmpty) {
              "< no tables found >"
            } else {
              "Database contains the following tables and views: " +
                "[ " + Util.joinWithComma(foundTables) + " ]"
            }
          }

          "\n\t" + foundTablesStr
        } else {
          ""
        }
      }

      String.format(fmtStr,
        tableName,
        srcDir.getAbsolutePath,
        testDir.getAbsolutePath,
        jdbc,
        generatorSettings,
        allTablesStr)
    }

    val config = generatorConfig(srcDir, testDir, generatorSettings)
    Class.forName(jdbc.driver) // load specified jdbc driver
    val model = Model(jdbc.url, jdbc.username, jdbc.password)
    model.table(jdbc.schema, tableName)
      .orElse(model.table(jdbc.schema, tableName.toUpperCase(en)))
      .orElse(model.table(jdbc.schema, tableName.toLowerCase(en)))
      .map { table =>
        Option(new CodeGenerator(table, className)(config))
      } getOrElse {
        logger.error(errMessage(model, printAllTables = true))
        None
      }
  }

  private def allTables(includeViews: Boolean,
                        jdbc: JDBCSettings): Seq[Table] = {
    allTables(includeViews, jdbc, Model(jdbc.url, jdbc.username, jdbc.password))
  }

  private def allTables(includeViews: Boolean,
                        jdbc: JDBCSettings,
                        model: Model): Seq[Table] = {

    def views = {
      if(includeViews) {
        model.allViews(jdbc.schema)
      } else {
        Seq()
      }
    }

    Class.forName(jdbc.driver)
    model.allTables(jdbc.schema) ++ views
  }

  def allGenerators(srcDir: File, testDir: File, jdbc: JDBCSettings, generatorSettings: GeneratorSettings): collection.Seq[CodeGenerator] = {
    val config = generatorConfig(srcDir, testDir, generatorSettings)
    val className = None
    Class.forName(jdbc.driver) // load specified jdbc driver
    val model = Model(jdbc.url, jdbc.username, jdbc.password)
    val tableAndViews = if (generatorSettings.view) {
      model.allTables(jdbc.schema) ++ model.allViews(jdbc.schema)
    } else {
      model.allTables(jdbc.schema)
    }

    tableAndViews.map { table =>
      new CodeGenerator(table, className)(config)
    }
  }

  def using[R <: {def close()}, A](resource: R)(f: R => A): A = ultimately {
    ignoring(classOf[Throwable]) apply resource.close()
  } apply f(resource)

}

object ScalikeJDBCMapperGenerator {
   object Keys {
     private final val JDBC = "jdbc."
     final val JDBC_DRIVER = JDBC + "driver"
     final val JDBC_URL = JDBC + "url"
     final val JDBC_USER_NAME = JDBC + "username"
     final val JDBC_PASSWORD = JDBC + "password"
     final val JDBC_SCHEMA = JDBC + "schema"

     private final val GENERATOR = "generator."
     final val PACKAGE_NAME = GENERATOR + "packageName"
     final val TEMPLATE = GENERATOR + "template"
     final val TEST_TEMPLATE = GENERATOR + "testTemplate"
     final val LINE_BREAK = GENERATOR + "lineBreak"
     final val ENCODING = GENERATOR + "encoding"
     final val AUTO_CONSTRUCT = GENERATOR + "autoConstruct"
     final val DEFAULT_AUTO_SESSION = GENERATOR + "defaultAutoSession"
     final val DATETIME_CLASS = GENERATOR + "dateTimeClass"
     final val RETURN_COLLECTION_TYPE = GENERATOR + "returnCollectionType"
     final val VIEW = GENERATOR + "view"
     final val TABLE_NAMES_TO_SKIP = GENERATOR + "tableNamesToSkip"
     final val BASE_TYPES = GENERATOR + "baseTypes"
     final val COMPANION_BASE_TYPES = GENERATOR + "companionBaseTypes"

     final val jdbcKeys: Set[String] = Set(
       JDBC_DRIVER, JDBC_URL, JDBC_USER_NAME, JDBC_PASSWORD, JDBC_SCHEMA)
     final val generatorKeys: Set[String] = Set(
       PACKAGE_NAME, TEMPLATE, TEST_TEMPLATE, LINE_BREAK,
       ENCODING, AUTO_CONSTRUCT, DEFAULT_AUTO_SESSION, DATETIME_CLASS, RETURN_COLLECTION_TYPE,
       VIEW, TABLE_NAMES_TO_SKIP, BASE_TYPES, COMPANION_BASE_TYPES)
     final val allKeys: Set[String] = jdbcKeys ++ generatorKeys
   }

  object CheckKey {
    private val matchPunctuation = Pattern.compile("\\p{Punct}")

    private def normalize(s: String): String = {
      //remove punctuation
      matchPunctuation.matcher(s.toLowerCase)
        .replaceAll("")
    }

    private lazy val normalizedKeys: Map[String, String] = {
      Keys.allKeys.map(k => (normalize(k), k))
    }.toMap

    private def recommendation(key: String): Option[String] =
      normalizedKeys.get(key).map(x => s"Did you mean $x?")

    def checkKey(key: String): Unit = {
      if(!Keys.allKeys.contains(key)) {
        throw new MapperGeneratorConfigException(
          s"$key is not a valid mapper generator configuration key" +
            recommendation(key).map(r => ". " + r).getOrElse("")
          )
      }
    }

    def checkKeys(props: Properties): Unit = {
      val propKeys: Set[String] = JavaConverters
        .asScalaSet(props.keySet())
        .toSet
        //should we check the types of keys?
        .map((x: Any) => x.toString)

      val wrongKeys = propKeys.diff(Keys.allKeys)

      if(wrongKeys.nonEmpty) {
        throw new MapperGeneratorConfigException(
          s"$wrongKeys are invalid mapper generator configuration keys")
      }
    }
  }


  private def getString(props: Properties)(key: String): Option[String] =
    Option(props.get(key)).map { value =>
      val str = value.toString
      if (str.startsWith("\"") && str.endsWith("\"") && str.length >= 2) {
        str.substring(1, str.length - 1)
      } else str
    }

  /**
   * unfortunately DateTimeClass's all and map fields are private
   * so we have to duplicate them here
   * same goes for ReturnCollectionType
   *
   * An easy way to do this automatically in vim:
   *  1. go to the class
   *  2. using visual line (shift-v), highlight the lines defining the case objects, e.g:
   *  case object List extends ReturnCollectionType("list")
   *  3. press colon (you should see :'<,'> appear in the message line)
   *  4. copy and paste the following regex and press enter:
   *     s/^\(\s*\)\(case\s\+object\s\+\)\([[:alnum:]_]\+\)\(\s*extends\s*[[:alnum:]_]\+\)(\("[[:alnum:]_]*"\))/\1\5 -> \3,/g
   * @return
   */
  private def dateTimeClassMap: Map[String, DateTimeClass] = {
    import DateTimeClass._
    Map(
      "org.joda.time.DateTime" -> JodaDateTime,
      "java.time.ZonedDateTime" -> ZonedDateTime,
      "java.time.OffsetDateTime" -> OffsetDateTime,
      "java.time.LocalDateTime" -> LocalDateTime
    )
  }

  private def returnCollectionTypeMap: Map[String, ReturnCollectionType] = {
    import ReturnCollectionType._
    Map(
      "list" -> List,
      "vector" -> Vector,
      "array" -> Array,
      "factory" -> Factory
    )
  }

  private def commaSeparated(props: Properties, key: String): collection.Seq[String] =
    getString(props)(key).map(_.split(',').map(_.trim).filter(_.nonEmpty).toList).getOrElse(Nil)

  class MapperGeneratorConfigException(override val msg: String)
    extends MapperException(msg)
}