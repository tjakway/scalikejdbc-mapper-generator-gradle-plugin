package org.grimrose.gradle.scalikejdbc.util

import java.util.Properties

import scala.collection.JavaConverters


object MergeProperties {
  abstract class ResolvePropertiesConflict {
    def apply(left: (String, String),
              right: (String, String)):
    (String, String)
  }

  //TODO: change to Object to not mangle Properties that don't use strings
  object ResolvePropertiesConflict {
    case class PropertiesConflictException(val msg: String)
      extends RuntimeException(msg)

    object PreferLeft extends ResolvePropertiesConflict {
      override def apply(left: (String, String),
                         right: (String, String)):
      (String, String) = left
    }

    object PreferRight extends ResolvePropertiesConflict {
      override def apply(left: (String, String),
                         right: (String, String)):
      (String, String) = right
    }

    class ThrowException extends ResolvePropertiesConflict {
      protected def mkErrorMessage(left: (String, String),
                                   right: (String, String)): String = {
        "Found nonunique keys while merging java.util.Properties objects:" +
          s"left=$left, right=$right"
      }

      protected def mkThrowable(msg: String): Throwable =
        PropertiesConflictException(msg)

      override def apply(left: (String, String),
                         right: (String, String)):
      (String, String) = {
        throw mkThrowable(mkErrorMessage(left, right))
      }
    }

    lazy val throwException: ThrowException = new ThrowException()
  }

  private def merge(resolvePropertiesConflict: ResolvePropertiesConflict)
                   (mergeInto: Properties,
                    toMerge: Properties,
                    checkWith: Properties,
                    isLeft: Boolean): Properties = {

    JavaConverters.asScalaSet(toMerge.keySet()).foreach { thisKeyObject =>
      val thisKey: String = thisKeyObject.toString

      //check that our key is unique
      Option(checkWith.getProperty(thisKey)) match {
        case Some(found) => {
          val first: (String, String) = (thisKey, toMerge.getProperty(thisKey))
          val second: (String, String) = (thisKey, found)
          val toInsert: (String, String) = if(isLeft) {
            resolvePropertiesConflict(first, second)
          } else {
            resolvePropertiesConflict(second, first)
          }

          mergeInto.setProperty(toInsert._1, toInsert._2)
        }

        case None => mergeInto.setProperty(
          thisKey, toMerge.getProperty(thisKey))
      }
    }

    mergeInto
  }

  def apply(resolvePropertiesConflict: ResolvePropertiesConflict)
           (left: Properties, right: Properties): Properties = {
    val props = new Properties()
    def mergeF = merge(resolvePropertiesConflict)(props, _, _, _)

    //merge left then right, checking against the other each time for uniqueness
    mergeF(left, right, true)
    mergeF(right, left, false)
  }
}
