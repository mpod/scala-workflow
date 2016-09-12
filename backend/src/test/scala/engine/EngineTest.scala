package engine

import org.scalatest.FlatSpec

class EngineTest extends FlatSpec {

  behavior of "An engine"

  it should "be less" in {
    assert(2 < 3)
  }

}
