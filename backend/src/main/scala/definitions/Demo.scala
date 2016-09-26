package definitions

import engine.ActionResult.{No, Ok, Yes}
import engine.ManualTaskDefinition.{IntField, StringField}
import engine.TaskDefinition._
import engine.{ActionResult, ManualTaskDefinition, TaskDefinition, WorkflowDefinition}

object Demo extends WorkflowDefinition {
  val manual1 = new ManualTaskDefinition(List(
    IntField("Simple Int field", "intfield")
  ))
  val manual2 = new ManualTaskDefinition(List(
    StringField("Simple String field", "strfield")
  ))
  val branch = new BranchTaskDefinition(context => {
    context.task.parent.get.value.get[Int]("intfield").get > 100
  })
  val subflow = new SubWorkflowTaskDefinition(Demo)
  val wait1 = new WaitFirstTaskDefinition(subflow, branch)
  val wait2 = new WaitAllTaskDefinition(wait1, manual2)

  override val transitions: Map[(TaskDefinition, ActionResult), List[TaskDefinition]] = Map(
    (StartTaskDefinition, Ok) -> List(manual1, manual2),
    (manual1, Ok) -> List(branch),
    (branch, Yes) -> List(subflow),
    (branch, No) -> List(wait1),
    (subflow, Ok) -> List(wait1),
    (wait1, Ok) -> List(wait2),
    (manual2, Ok) -> List(wait2),
    (wait2, Ok) -> List(EndTaskDefinition)
  )
  override val name: String = "Demo"
}
