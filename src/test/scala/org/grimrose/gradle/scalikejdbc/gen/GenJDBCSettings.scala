package org.grimrose.gradle.scalikejdbc.gen

import org.grimrose.gradle.scalikejdbc.TestSettings
import org.grimrose.gradle.scalikejdbc.gen.GenSettings.genOptionalString
import org.grimrose.gradle.scalikejdbc.mapper.JDBCSettings
import org.scalacheck.Gen

import java.io.File

trait GenJDBCSettings {
  protected def genDriver(availableDrivers: TestSettings.DriverMap):
    Gen[(String, String)] = {
    Gen.oneOf(availableDrivers.toSeq)
  }

  /**
   *
   * @param driverPrefix as in, e.g, jdbc:driverPrefix:file:/path/to/db
   * @return
   */
  protected def genFileUrl(driverPrefix: String, dbLoc: File): Gen[String] = {
    GenJDBCSettings.mergeUrlComponents(
      Seq("jdbc", driverPrefix, "file", dbLoc.getAbsolutePath))
  }

  protected def genUsername: Gen[String]
  protected def genPassword: Gen[String]
  protected def genSchema: Gen[String]

  final def genJDBCSettings(availableDrivers: TestSettings.DriverMap)
                           (dbLoc: File): Gen[JDBCSettings] = {
    for {
      (driver, driverPrefix) <- genDriver(availableDrivers)
      url <- genFileUrl(driverPrefix, dbLoc)
      username <- genUsername
      password <- genPassword
      schema <- genSchema
    } yield {
      JDBCSettings(driver, url, username, password, schema)
    }
  }
}

object GenJDBCSettings {
  private def mergeUrlComponents(left: String, right: String) = {
    left + ":" + right
  }
  private def mergeUrlComponents(xs: Seq[String]): String = {
    xs.reduce(mergeUrlComponents)
  }

  trait GenStandardSettings extends GenJDBCSettings {
    //a Gen[String] that can't start with a number
    private def genAlphaStartingStr: Gen[String] =  {
      def f = for {
        firstChar <- Gen.alphaChar
        rest <- Gen.alphaNumStr
      } yield {
        firstChar + rest
      }
      genOptionalString(f)
    }

    override protected def genUsername: Gen[String] = genAlphaStartingStr
    override protected def genPassword: Gen[String] = Gen.alphaNumStr
    override protected def genSchema: Gen[String] = genAlphaStartingStr
  }

  /**
   * return a JDBCSettings object with empty username, password, and schema
   */
  trait GenEmptyAuthSettings extends GenJDBCSettings {
    private def empty: Gen[String] = Gen.const("")
    override protected def genUsername: Gen[String] = empty
    override protected def genPassword: Gen[String] = empty
    override protected def genSchema: Gen[String] = empty
  }
}