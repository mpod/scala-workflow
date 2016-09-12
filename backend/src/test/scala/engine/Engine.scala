package engine

import org.scalatest.FunSuite

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class Engine extends FunSuite {

  test("contains") {
    assert(2 < 3)
  }

}
