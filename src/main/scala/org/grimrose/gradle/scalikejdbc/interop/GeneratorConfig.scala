package org.grimrose.gradle.scalikejdbc.interop

import java.util.{ArrayList, Collection, Optional => JOptional}

import org.gradle.api.tasks.{Input, Optional}
import org.grimrose.gradle.scalikejdbc.interop.ToProperties.PropertyValue

import scala.beans.BeanProperty

class GeneratorConfig extends Serializable {
  @BeanProperty
  @Input
  @Optional
  var packageName: String =_
  @BeanProperty
  @Input
  @Optional
  var template: String = _
  @BeanProperty
  @Input
  @Optional
  var testTemplate: String = _
  @BeanProperty
  @Input
  @Optional
  var lineBreak: String = _
  @BeanProperty
  @Input
  @Optional
  var encoding: String = _
  @BeanProperty
  @Input
  @Optional
  var autoConstruct: JOptional[Boolean] = JOptional.empty()
  @BeanProperty
  @Input
  @Optional
  var defaultAutoSession: JOptional[Boolean] = JOptional.empty()
  @BeanProperty
  @Input
  @Optional
  var dateTimeClass: String = _
  @BeanProperty
  @Input
  @Optional
  var returnCollectionType: String = _
  @BeanProperty
  @Input
  @Optional
  var view: JOptional[Boolean] = JOptional.empty()
  @BeanProperty
  @Input
  @Optional
  var tableNamesToSkip: Collection[String] = new ArrayList()
  @BeanProperty
  @Input
  @Optional
  var baseTypes: Collection[String] = new ArrayList()
  @BeanProperty
  @Input
  @Optional
  var companionBaseTypes: Collection[String] = new ArrayList()

  //TODO: add fields for tableNameToSyntaxName and tableNameToSyntaxVariableName
  //and add support in ScalikeJDBCMapperGenerator.Keys

  override def toString: String = {
    getClass.getName + "(" + GeneratorConfig.toAnnotatedMap(this) + ")"
  }
}

object GeneratorConfig extends ToProperties[GeneratorConfig] {
  override def toAnnotatedMap(in: GeneratorConfig):
    Map[String, ToProperties.PropertyValue] = {

    import ToProperties.PropertyValue.{OptionalPropertyValue, StringValue}
    import org.grimrose.gradle.scalikejdbc.mapper.ScalikeJDBCMapperGenerator.Keys._
    Map(
      PACKAGE_NAME -> StringValue(in.packageName),
      TEMPLATE -> StringValue(in.template),
      TEST_TEMPLATE -> StringValue(in.testTemplate),
      LINE_BREAK -> StringValue(in.lineBreak),
      ENCODING -> StringValue(in.encoding),
      AUTO_CONSTRUCT -> OptionalPropertyValue(in.autoConstruct),
      DEFAULT_AUTO_SESSION -> OptionalPropertyValue(in.defaultAutoSession),
      DATETIME_CLASS -> StringValue(in.dateTimeClass),
      RETURN_COLLECTION_TYPE -> StringValue(in.returnCollectionType),
      VIEW -> OptionalPropertyValue(in.view),
      TABLE_NAMES_TO_SKIP -> PropertyValue.Collection(in.tableNamesToSkip),
      BASE_TYPES -> PropertyValue.Collection(in.baseTypes),
      COMPANION_BASE_TYPES -> PropertyValue.Collection(in.companionBaseTypes)
    )
  }
}
