package org.grimrose.gradle.scalikejdbc.gen

import org.scalacheck.Gen

import scala.annotation.tailrec

trait GenCommon {
  def optional[A](zero: A)(g: Gen[A]): Gen[A] = {
    Gen.option(g).map(_.getOrElse(zero))
  }

  def shuffle[A](sequence: Seq[A]): Gen[Seq[A]] = {
    import scala.collection.immutable.{List => CList}
    /*
    def helper(xs: Seq[A], acc: CList[A]): Gen[Seq[A]] = {
      if(xs.isEmpty) {
        acc
      } else {
        //note: choose is inclusive
        //see https://github.com/typelevel/scalacheck/blob/master/src/main/scala/org/scalacheck/Gen.scala
        val genChoice: Gen[Int] = Gen.choose(0, xs.size)
        genChoice.
      }
    }
     */

    def helper(rest: Seq[A], acc: CList[A]): Gen[Seq[A]] = {
        if(rest.isEmpty) {
          Gen.const(acc)
        } else {
            pickOne(rest).flatMap { x =>
              val (chosen, remainder) = x
              //since we're shuffling it doesn't matter if the order
              //is reversed
              //but could always .reverse it at the end

              //since rest is nonempty, head should be safe
              val newAcc = chosen.get :: acc

              helper(remainder, newAcc)
        }
      }
    }

    helper(sequence, CList.empty)
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
        //slice out the given index
        //the index we sliced on is the first element in the tail
        //i.e:

        //  scala> Seq(5, 6, 4, 3).splitAt(1)
        //  res1: (Seq[Int], Seq[Int]) = (List(5),List(6, 4, 3))
        //
        //  scala> Seq(1).splitAt(0)
        //  res2: (Seq[Int], Seq[Int]) = (List(),List(1))

        val (head, rest) = xs.splitAt(chosenIndex)

        //we've checked that the list isn't empty
        assert(rest.nonEmpty)

        val chosenItem = rest.head
        val tail = rest.tail

        (Some(chosenItem), head ++ tail)
      }
    }
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