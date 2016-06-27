import engine._
import definitions._

object Workflow {
  def main(args: Array[String]): Unit = {
    Engine.startWorkflow(RandomWorkflow)

    for (i <- 1 to 10) {
      println("Iteration " + i)
      println(Engine.executeRound)
    }
  }
}
