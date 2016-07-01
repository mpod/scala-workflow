import engine._
import definitions._

object Workflow {
  def main(args: Array[String]): Unit = {
    val wf: Workflow = Engine.startWorkflow(RandomWorkflow)

    WorkflowCacheService.put(wf, "name", 1)
    WorkflowCacheService.put(wf, "name", 2)

    for (i <- 1 to 10) {
      println("Iteration " + i)
      Engine.executeRound
    }
  }
}
