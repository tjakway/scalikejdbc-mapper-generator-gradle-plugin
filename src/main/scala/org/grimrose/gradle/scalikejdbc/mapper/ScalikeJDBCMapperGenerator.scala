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

import org.gradle.api.Project
import org.grimrose.gradle.scalikejdbc.interop.ToProperties
import org.grimrose.gradle.scalikejdbc.util.MergeProperties.ResolvePropertiesConflict
import org.grimrose.gradle.scalikejdbc.util.MergeProperties.ResolvePropertiesConflict.PreferLeft
import org.grimrose.gradle.scalikejdbc.util.{MergeProperties, Util}
import org.grimrose.gradle.scalikejdbc.{MapperException, interop}
import org.slf4j.{Logger, LoggerFactory}
import scalikejdbc.mapper._

import java.io.File
import java.util.Locale.{ENGLISH => en}
import java.util.Properties
import java.util.regex.Pattern
import scala.collection.JavaConverters

/**
 * ScalikeJDBC Mapper Generator
 *
 * @see scalikejdbc.mapper.SbtPlugin
 */
class ScalikeJDBCMapperGenerator(val onPropertiesFilePermissionError:
                                   String => Unit =
                                 ScalikeJDBCMapperGenerator
                                   .throwOnPropertiesFilePermissionError) {
  import ScalikeJDBCMapperGenerator._
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private def resolveSettings(
                      getPropertiesFile: Project => Properties)(
                      project: Project,
                      jdbcConfig: interop.JdbcConfig,
                      generatorConfig: interop.GeneratorConfig,
                      loadPropertiesSetting: LoadPropertiesSetting):
    (JDBCSettings, GeneratorSettings) = {

    def mergedConfigs: Properties =
      ToProperties.mergeConfigs(jdbcConfig, generatorConfig)

    val resolvedProperties: Properties =
      MergeProperties.apply(
        loadPropertiesSetting.resolvePropertiesConflict)(
        mergedConfigs,
        getPropertiesFile(project))

    settingsFromProperties(resolvedProperties)
  }

  type ResolveSettingsF = (Project,
    interop.JdbcConfig,
    interop.GeneratorConfig,
    LoadPropertiesSetting) =>
    (JDBCSettings, GeneratorSettings)

  /**
   * resolve settings using default .properties file paths
   * @return
   */
  def resolveSettingsWithDefaultPaths: ResolveSettingsF =
    resolveSettings((project: Project) =>
      loadAllProperties(project, DefaultPropertyPaths.all))

  def resolveSettingsWithPropertyFiles(
    usePropertyFiles: Seq[File]): ResolveSettingsF =
    resolveSettings((ignored: Project) => loadAllProperties(usePropertyFiles))

  private def loadAllProperties(loadFrom: Seq[File]): Properties = {
    def load(from: File): Properties = {
      val props = new Properties()
      Util.using(new java.io.FileInputStream(from)) {
        inputStream => props.load(inputStream)
      }
      props
    }

    def checkAttrs(f: File): Option[File] = {
      val x = Option(f).filter(n => n.exists && n.isFile)

      x.foreach { n =>
        if(!n.canRead) {
          val msg: String =
            s"${f} exists and is a file but cannot be read (check permissions)"
          onPropertiesFilePermissionError(msg)
        }
      }

      x.filter(_.canRead)
    }

    def getProperties(g: File): Option[Properties] = {
      val optF = Option(g)
      optF match {
        case Some(f) =>
          Option(f)
            .flatMap(checkAttrs)
            .map(load)
        case None => {
          val optFStr: String =
            optF.map(_.getAbsolutePath).getOrElse("< null path >")
          logger.debug(
            s"Could not find properties file at $optFStr")
          None
        }
      }
    }

    val start: Properties = new Properties()
    loadFrom.foldLeft(start) {
      case (acc, thisLoc) => {
        //if we can't find the file,
        //merge using an empty properties object
        val loadedProperties = getProperties(thisLoc)
          .getOrElse(new Properties())
        MergeProperties.apply(PreferLeft)(acc, loadedProperties)
      }
    }
  }

  /**
   *
   * @param project
   * @param loadFrom paths to .properties files,
   *                 in order from highest -> lowest priority
   * @return
   */
  private def loadAllProperties(project: Project,
                                loadFrom: Seq[String]): Properties = {
    loadAllProperties(loadFrom.map(project.file))
  }

  def settingsFromProperties(props: Properties): (JDBCSettings, GeneratorSettings) = {
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

  protected def allTables(includeViews: Boolean,
                        jdbc: JDBCSettings): Seq[Table] = {
    allTables(includeViews, jdbc, Model(jdbc.url, jdbc.username, jdbc.password))
  }

  protected def allTables(includeViews: Boolean,
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
}

object ScalikeJDBCMapperGenerator {
  private lazy val logger: Logger = LoggerFactory.getLogger(getClass)

  def apply(failOnPermissionError: Boolean = true):
    ScalikeJDBCMapperGenerator = {
    val selectF = {
      if(failOnPermissionError) {
        throwOnPropertiesFilePermissionError
      } else {
        logOnPropertiesFilePermissionError
      }
    }
    new ScalikeJDBCMapperGenerator(selectF)
  }

  /**
   * relative to the project directory
   */
  object DefaultPropertyPaths {
    val mapperGeneratorProperties: String = {
      "project" + File.separator + "scalikejdbc-mapper-generator.properties"
    }

    val scalikejdbcProperties: String =
      "project" + File.separator + "scalikejdbc.properties"

    val all: Seq[String] = Seq(mapperGeneratorProperties, scalikejdbcProperties)
  }

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

  sealed trait LoadPropertiesSetting {
    def resolvePropertiesConflict: ResolvePropertiesConflict
  }
  object LoadPropertiesSetting {
    def default: LoadPropertiesSetting = PreferGradle

    case object PreferGradle extends LoadPropertiesSetting {
      override def resolvePropertiesConflict: ResolvePropertiesConflict =
        ResolvePropertiesConflict.PreferLeft
    }

    case object PreferPropertiesFile extends LoadPropertiesSetting {
      override def resolvePropertiesConflict: ResolvePropertiesConflict =
        ResolvePropertiesConflict.PreferRight
    }

    case object AssertNoConflict extends LoadPropertiesSetting {
      override def resolvePropertiesConflict: ResolvePropertiesConflict =
        new ResolvePropertiesConflict.ThrowException {
          override protected def mkThrowable(msg: String): Throwable =
            DuplicatePropertiesException(msg)
        }
    }
  }

  private def commaSeparated(props: Properties, key: String): collection.Seq[String] =
    getString(props)(key).map(_.split(',').map(_.trim).filter(_.nonEmpty).toList).getOrElse(Nil)

  class MapperGeneratorConfigException(override val msg: String)
    extends MapperException(msg)

  case class DuplicatePropertiesException(conflictMsg: String)
    extends MapperGeneratorConfigException(
      String.format("User asserted no duplicates between " +
        "gradle-defined settings and mapper property files\n%s", conflictMsg))

  case class PropertiesFilePermissionException(override val msg: String)
    extends MapperException(msg)

  def throwOnPropertiesFilePermissionError: String => Unit = { msg =>
    throw PropertiesFilePermissionException(msg)
  }

  def logOnPropertiesFilePermissionError: String => Unit = { msg =>
    logger.error(msg)
  }
}