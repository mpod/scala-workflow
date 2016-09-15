package engine

import org.scalatest.FlatSpec

class TaskDefinitionTest extends FlatSpec {

  def fixture =
    new {
      val builder = new StringBuilder("ScalaTest is ")
      val buffer = new ListBuffer[String]
    }

}
