package org.grimrose.gradle.scalikejdbc.mapper

import scalikejdbc.mapper.{DateTimeClass, ReturnCollectionType}

case class GeneratorSettings(
          packageName: String,
          template: String,
          testTemplate: String,
          lineBreak: String,
          encoding: String,
          autoConstruct: Boolean,
          defaultAutoSession: Boolean,
          dateTimeClass: DateTimeClass,
          tableNameToClassName: String => String,
          columnNameToFieldName: String => String,
          returnCollectionType: ReturnCollectionType,
          view: Boolean,
          tableNamesToSkip: collection.Seq[String],
          baseTypes: collection.Seq[String],
          companionBaseTypes: collection.Seq[String],
          tableNameToSyntaxName: String => String,
          tableNameToSyntaxVariableName: String => String)