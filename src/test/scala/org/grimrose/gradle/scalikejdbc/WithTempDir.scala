package org.grimrose.gradle.scalikejdbc

import org.grimrose.gradle.scalikejdbc.WithTempDir.MakeTempDirException

import java.io.File
import java.nio.file.Files
import scala.util.{Failure, Success, Try}

trait WithTempDir {
  protected def tempDirPrefix: Option[String] =
    Some(getClass
      .getCanonicalName
      .replaceAll("""[^\p{Alnum}]""", "_"))

  private lazy val tempDir: Try[File] =
    Try(Files.createTempDirectory(tempDirPrefix.orNull).toFile)

  private def getTempDir: File = tempDir match {
    case Success(x) => x
    case Failure(t) => throw MakeTempDirException(t, tempDirPrefix)
  }

  def getTempFile(inDir: File = getTempDir,
                  prefix: Option[String] = None,
                  suffix: Option[String] = None): File = {
    Files.createTempFile(inDir.toPath, prefix.orNull, suffix.orNull).toFile
  }

  def withTempDir[A](f: File => A): A = f(getTempDir)
}

object WithTempDir {
  case class MakeTempDirException(msg: String)
    extends RuntimeException(msg)

  object MakeTempDirException {
    def apply(t: Throwable,
              prefix: Option[String]): MakeTempDirException = {
      val ex = MakeTempDirException(
        "Error creating temporary directory" +
          prefix.map(p => " with prefix " + p).getOrElse(""))

      ex.initCause(t)
      ex
    }
  }
}
