package part1

import scala.util.Success
import scala.util.Failure
import scala.util.Try
import scala.concurrent.java8.FuturesConvertersImpl.P

object ScalaRecap {

  val pf : PartialFunction[Try[String], Unit] = {
    case Success(value) =>
        println(s"the value is $value")
    case Failure(e) =>
        println(s"exception $e")
  }

  val s = Success("great")
  val f = Failure(new RuntimeException("oops"))

  val pi = new PartialFunction[Int, Double] {
    def isDefinedAt(x: Int): Boolean = x != 0
    def apply(x: Int): Double = 1.0 / x
  }
  
  def main(args: Array[String]) {
    println("Scala Recap")

    pf {
        s
    }

    pf {
        f
    }

    println(pi(100))
  }
}
