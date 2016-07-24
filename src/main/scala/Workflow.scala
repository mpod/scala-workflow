import engine._
import definitions._

object Workflow {
  def main(args: Array[String]): Unit = {
    implicit val idGen = SimpleIdGenerator
    val engine = new Engine()
    val wf = engine.startWorkflow(ExampleWorkflow)
    var i = 1

    while (!wf.allExecuted) {
      println("Iteration %d".format(i))
      engine.executeRound
      i += 1
    }
  }
}


