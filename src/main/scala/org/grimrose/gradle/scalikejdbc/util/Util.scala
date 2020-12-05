package org.grimrose.gradle.scalikejdbc.util

import java.util.Properties
import java.util.{Optional => JOptional}

import scala.collection.JavaConverters

object Util {
  private def callToString[A]: A => String = (a: A) => a.toString
  def joinWithSeparator[A](xs: Iterable[A],
                           separator: String,
                           printF: A => String = callToString): String = {
    if(xs.isEmpty) {
      ""
    } else {
      val sb = new java.lang.StringBuilder()
      xs.foreach { x =>
        sb.append(printF(x))
        sb.append(separator)
      }

      val joinedStr = sb.toString

      //dont need to remove the last separator if there isn't one
      if(separator.nonEmpty) {
        val index = joinedStr.lastIndexOf(separator)
        //we inserted at least one separator so lastIndexOf should
        //never be negative
        assert(index > 0)
        assert(joinedStr.length > (index - 1))
        joinedStr.substring(0, index)
      } else {
        joinedStr
      }
    }
  }

  def joinWithSeparator[A](xs: java.util.Collection[A],
                           separator: String): String =
    joinWithSeparator(xs, separator, callToString[A])

  def joinWithSeparator[A](xs: java.util.Collection[A],
                           separator: String,
                           printF: A => String): String = {
    val iterable: Iterable[A] =
      JavaConverters.asScalaIterator(xs.iterator()).toIterable
    joinWithSeparator(iterable, separator, printF)
  }

  def joinWithComma[A](xs: Iterable[A],
                       printF: A => String = callToString): String =
    joinWithSeparator[A](xs, ", ", printF)

  def mapToProperties(xs: Map[String, String]): Properties = {
    val props = new Properties()
    xs.foreach {
      case (key, value) => props.setProperty(key, value)
    }

    if(xs.nonEmpty) {
      assert(!props.isEmpty)
    }

    props
  }

  def asScalaOption[A](x: JOptional[A]): Option[A] = {
    if(x.isPresent) {
      Some(x.get())
    } else {
      None
    }
  }

  def asJavaOptional[A](x: Option[A]): JOptional[A] = x match {
    case Some(a) => JOptional.of(a)
    case None => JOptional.empty()
  }

  def optionalString(x: String): Option[String] =
    Option(x).filter(_.trim.nonEmpty)


  class CloseFailedException(val msg: String)
    extends RuntimeException(msg)

  object CloseFailedException {
    def onCloseErr(msg: String,
                   causedBy: Option[Throwable]): Throwable = {
      val ex = new CloseFailedException(msg)
      causedBy.foreach(ex.initCause)
      ex
    }
    def onCloseErr(msg: String): Throwable =
      onCloseErr(msg, None)
    def onCloseErr(msg: String,
                   throwable: Throwable): Throwable =
      onCloseErr(msg, Some(throwable))
  }

  def using[R <: {def close()}, A](resource: R)(f: R => A): A = {
    import scala.language.reflectiveCalls
    val resResult: Either[Throwable, A] = try {
      Right(f(resource))
    } catch {
      case t: Throwable => {
        Left(t)
      }
    }

    //try and close the resource
    //if that fails, add any previous errors
    //to the list of suppressed exceptions
    try {
      resource.close()
    } catch {
      case t: Throwable => {
        val closeFailedException = CloseFailedException.onCloseErr(
          s"Failed to close resource $resource",
          t)

        resResult match {
          case Left(prevError) => {
            closeFailedException.addSuppressed(prevError)
          }
          case _ => {}
        }

        throw closeFailedException
      }
    }

    //if we haven't thrown an exception yet, handle the original result
    //and throw if necessary
    resResult match {
      case Right(res) => res
      case Left(err) => throw err
    }
  }
}
