package org.grimrose.gradle.scalikejdbc.interop

import java.util.Properties
import java.util.Collection
import java.util.{Optional => JOptional}

import org.grimrose.gradle.scalikejdbc.util.Util

import scala.collection.JavaConverters

trait ToProperties[A] {
  def toProperties(in: A): Properties = {
    //filter null or empty values
    val map = toMap(in).filter {
      case (_, value) =>
        Option(value).exists(v => v.trim.nonEmpty)
    }

    Util.mapToProperties(map)
  }

  def formatCollection(x: Collection[String]): String =
    Util.joinWithSeparator(x, ",")

  def toMap(in: A): Map[String, String] = {
    val empty: Map[String, String] = Map.empty
    toAnnotatedMap(in).foldLeft(empty) {
          //only include values that return Some in the map
      case (acc, (key, propertyValue)) => {
        propertyValue.renderPropertyValue match {
          case Some(value) => acc.updated(key, value)
          case None => acc
        }
      }
    }
  }

  def toAnnotatedMap(in: A): Map[String, ToProperties.PropertyValue]
}

object ToProperties {
  private def asScalaOption[A](x: JOptional[A]): Option[A] = {
    if(x.isPresent) {
      Some(x.get())
    } else {
      None
    }
  }

  sealed trait PropertyValue {
    def renderPropertyValue: Option[String]
  }

  object PropertyValue {
    case class OptionalPropertyValue[A](value: Option[A])
      extends PropertyValue {
      override def renderPropertyValue: Option[String] = value.map(_.toString)
    }
    object OptionalPropertyValue {
      def apply[A](value: JOptional[A]): OptionalPropertyValue[A] =
        OptionalPropertyValue(asScalaOption(value))
    }

    case class Collection[A](coll: java.util.Collection[A])
      extends PropertyValue {

      override def renderPropertyValue: Option[String] = {
        import JavaConverters.collectionAsScalaIterableConverter
        Option(coll)
          .filterNot(_.isEmpty)
          .map(c => Util.joinWithSeparator(c.asScala, ","))
          .filterNot(_.trim.isEmpty)
      }
    }

    case class StringValue(value: Option[String])
      extends PropertyValue {

      override def renderPropertyValue: Option[String] = value
    }

    object StringValue {
      def apply(value: String): StringValue =
        StringValue(filter(value))

      //convert null or empty strings -> None
      private def filter(x: String): Option[String] =
        Option(x).filter(_.trim.nonEmpty)
    }
  }
}