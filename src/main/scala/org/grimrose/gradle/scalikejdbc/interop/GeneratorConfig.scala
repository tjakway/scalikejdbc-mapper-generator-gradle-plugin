package org.grimrose.gradle.scalikejdbc.interop

import java.util.{ArrayList, Collection, Properties}

import org.gradle.api.tasks.Input
import org.grimrose.gradle.scalikejdbc.util.Util

import scala.beans.BeanProperty

class GeneratorConfig {
  @BeanProperty
  @Input
  var packageName: String =_
  @BeanProperty
  @Input
  var template: String = _
  @BeanProperty
  @Input
  var testTemplate: String = _
  @BeanProperty
  @Input
  var lineBreak: String = _
  @BeanProperty
  @Input
  var encoding: String = _
  @BeanProperty
  @Input
  var autoConstruct: Boolean = _
  @BeanProperty
  @Input
  var defaultAutoSession: Boolean = _
  @BeanProperty
  @Input
  var dateTimeClass: String = _
  @BeanProperty
  @Input
  var returnCollectionType: String = _
  @BeanProperty
  @Input
  var view: Boolean = _
  @BeanProperty
  @Input
  var tableNamesToSkip: Collection[String] = new ArrayList()
  @BeanProperty
  @Input
  var baseTypes: Collection[String] = new ArrayList()
  @BeanProperty
  @Input
  var companionBaseTypes: Collection[String] = new ArrayList()

  //TODO: add fields for tableNameToSyntaxName and tableNameToSyntaxVariableName
  //and add support in ScalikeJDBCMapperGenerator.Keys
}

object GeneratorConfig extends ToProperties[GeneratorConfig] {
  override def toMap(in: GeneratorConfig): Map[String, String] = {
    import org.grimrose.gradle.scalikejdbc.mapper.ScalikeJDBCMapperGenerator.Keys._
    Map(
      PACKAGE_NAME -> in.packageName,
      TEMPLATE -> in.template,
      TEST_TEMPLATE -> in.testTemplate,
      LINE_BREAK -> in.lineBreak,
      ENCODING -> in.encoding,
      AUTO_CONSTRUCT -> in.autoConstruct.toString,
      DEFAULT_AUTO_SESSION -> in.defaultAutoSession.toString,
      DATETIME_CLASS -> in.dateTimeClass,
      RETURN_COLLECTION_TYPE -> in.returnCollectionType,
      VIEW -> in.view.toString,
      TABLE_NAMES_TO_SKIP -> formatCollection(in.tableNamesToSkip),
      BASE_TYPES -> formatCollection(in.baseTypes),
      COMPANION_BASE_TYPES -> formatCollection(in.companionBaseTypes)
    )
  }
}
