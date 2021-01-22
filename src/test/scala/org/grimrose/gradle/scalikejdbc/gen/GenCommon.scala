package org.grimrose.gradle.scalikejdbc.gen

import org.scalacheck.Gen

trait GenCommon {
  def genOptionalString(g: Gen[String]): Gen[String] =
    Gen.option(g).map(_.getOrElse(""))
}

object GenCommon extends GenCommon