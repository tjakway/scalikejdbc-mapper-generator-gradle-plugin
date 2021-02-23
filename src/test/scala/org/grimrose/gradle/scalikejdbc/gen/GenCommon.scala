package org.grimrose.gradle.scalikejdbc.gen

import org.scalacheck.Gen

trait GenCommon {
  def optional[A](zero: A)(g: Gen[A]): Gen[A] = {
    Gen.option(g).map(_.getOrElse(zero))
  }
  def shuffle[A](xs: Seq[A]): Gen[Seq[A]] = {
    if(xs.isEmpty) {
      Gen.const(Seq.empty)
    } else {
      val empty: Gen[(Set[Int], Seq[A])] = Gen.const((Set.empty, Seq.empty))
      val indicesSet = xs.indices.toSet
      val res = xs.indices.foldLeft(empty) {
        case (g, thisIndex) => g.flatMap {
          case (chosenIndices, acc) => {
            //if this ends up being too slow, cache remainingIndices
            //and update it with each new choice
            val remainingIndices = indicesSet.diff(chosenIndices)

            //shouldn't run out before the fold is over
            assert(remainingIndices.nonEmpty)
            Gen.oneOf(remainingIndices.toSeq).map { chosenIndex =>
              (chosenIndices + chosenIndex, acc :+ xs(chosenIndex))
            }
          }
        }
      }

      res.map(_._2)
    }
  }

  def pickOne[A](xs: Seq[A]): Gen[(Option[A], Seq[A])] = {
    //can't pick anything if it's empty
    if(xs.isEmpty) {
      Gen.const((None, Seq.empty))

    } else if(xs.size == 1) {
      //can only pick one thing if that's all there is
      Gen.const((xs.headOption, xs.tail))

    } else {
      //need to subtract 1 because choose is inclusive
      Gen.choose(0, xs.size - 1).map { chosenIndex =>

        val (item, rest) = slice(chosenIndex, xs)
        (Some(item), rest)
      }
    }
  }

  private def slice[A](index: Int, xs: Seq[A]): (A, Seq[A]) = {
    //slice out the given index
    //the index we sliced on is the first element in the tail
    //i.e:

    //  scala> Seq(5, 6, 4, 3).splitAt(1)
    //  res1: (Seq[Int], Seq[Int]) = (List(5),List(6, 4, 3))
    //
    //  scala> Seq(1).splitAt(0)
    //  res2: (Seq[Int], Seq[Int]) = (List(),List(1))
    assert(xs.nonEmpty)
    assert(index < xs.size)

    val (head, rest) = xs.splitAt(index)

    assert(rest.nonEmpty)

    val chosenItem = rest.head
    val tail = rest.tail

    (chosenItem, head ++ tail)
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