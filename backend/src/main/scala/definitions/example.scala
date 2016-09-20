package definitions

import engine.ActionResult.{No, Ok, Yes}
import engine.ManualTaskDefinition.{IntField, StringField}
import engine.Task.TaskContext
import engine.TaskDefinition._
import engine._

import util.Random

class WaitTaskDefinition(n: Int) extends TaskDefinition {
  /* TODO: Make it immutable */
  var waitFor = n

  override def action(implicit context: TaskContext): Option[ActionResult] = {
    waitFor -= 1
    if (waitFor <= 0)
      Some(Ok)
    else
      None
  }
  override def name: String = "Wait(%d)".format(n)
}

object ExampleWorkflow extends WorkflowDefinition {
  val man = new ManualTaskDefinition(List(
    StringField("Simple String field", "strfield"),
    IntField("Simple Int field", "intfield")
  ))
  val branch = new BranchTaskDefinition(context => new Random().nextBoolean)
  val wait2 = new WaitTaskDefinition(2)
  val wait3 = new WaitTaskDefinition(3)
  val join = new JoinTaskDefinition(Set(wait2, wait3))
  val subWf = new SubWorkflowTaskDefinition(ExampleSubWorkflow)
  val proc1 = new ProcessTaskDefinition(context => {
    val n = 2
    context.workflow.put("n", n)
    println("Writing %d to workflow cache".format(n))
  })
  val proc2 = new ProcessTaskDefinition(context => {
    println("Reading %d from workflow cache".format(context.workflow.get[Int]("n").get))
  })

  override val transitions: Map[(TaskDefinition, ActionResult), List[TaskDefinition]] = Map(
    (StartTaskDefinition, Ok) -> List(man),
    (man, Ok) -> List(branch),
    (branch, Yes) -> List(proc1),
    (proc1, Ok) -> List(wait2, wait3),
    (branch, No) -> List(subWf),
    (subWf, Ok) -> List(EndTaskDefinition),
    (wait2, Ok) -> List(join),
    (wait3, Ok) -> List(join),
    (join, Ok) -> List(proc2),
    (proc2, Ok) -> List(EndTaskDefinition)
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
