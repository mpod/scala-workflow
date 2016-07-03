import engine._
import definitions._

object Workflow {
  def main(args: Array[String]): Unit = {
    val wf = Engine.startWorkflow(ExampleWorkflow)
    var i = 1

    while (!wf.allExecuted) {
      println("Iteration %d".format(i))
      Engine.executeRound
      i += 1
    }
  }
}
