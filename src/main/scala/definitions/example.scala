package definitions

import engine._
import util.Random

class WaitTaskDefinition(n: Int) extends TaskDefinition {
  var waitFor = n

  override def action: Option[ActionResult] = {
    waitFor -= 1
    if (waitFor <= 0)
      Some(Ok)
    else
      None
  }
  override def name: String = "Wait(%d)".format(n)
}

object ExampleWorkflow extends WorkflowDefinition {
  //val branch = new BranchTaskDefinition(() => new Random().nextBoolean)
  val branch = new BranchTaskDefinition(() => false)
  val split = new SplitTaskDefinition()
  val wait2 = new WaitTaskDefinition(2)
  val wait3 = new WaitTaskDefinition(3)
  val join = new JoinTaskDefinition(2)
  val subWf = new SubWorkflowTaskDefinition(ExampleSubWorkflow)

  override val transitions: Map[(TaskDefinition, ActionResult), List[TaskDefinition]] = Map(
    (StartTaskDefinition, Ok) -> List(branch),
    (branch, Yes) -> List(split),
    (branch, No) -> List(subWf),
    (subWf, Ok) -> List(EndTaskDefinition),
    (split, Ok) -> List(wait2, wait3),
    (wait2, Ok) -> List(join),
    (wait3, Ok) -> List(join),
    (join, Ok) -> List(EndTaskDefinition)
  )
  override val name: String = "Example"
}

object ExampleSubWorkflow extends WorkflowDefinition {
  val wait5 = new WaitTaskDefinition(5)

  override val transitions: Map[(TaskDefinition, ActionResult), List[TaskDefinition]] = Map(
    (StartTaskDefinition, Ok) -> List(wait5),
    (wait5, Ok) -> List(EndTaskDefinition)
  )
  override val name: String = "Example Subflow"
}
