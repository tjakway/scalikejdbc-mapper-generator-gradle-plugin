package org.grimrose.gradle.scalikejdbc.gen

import org.scalacheck.Gen

trait GenCommon {
  def optional[A](zero: A)(g: Gen[A]): Gen[A] = {
    Gen.option(g).map(_.getOrElse(zero))
  }

  /**
   * a Gen[String] that can't start with a number
   */
  def genIdentifier: Gen[String] = {
    for {
      firstChar <- Gen.alphaChar
      rest <- Gen.alphaNumStr
    } yield {
      firstChar + rest
    }
  }
}

object GenCommon extends GenCommon