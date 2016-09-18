package definitions

import engine.ActionResult.{No, Ok, Yes}
import engine.ManualTaskDefinition.{IntField, StringField}
import engine.TaskDefinition._
import engine.{ActionResult, ManualTaskDefinition, TaskDefinition, WorkflowDefinition}

object Demo extends WorkflowDefinition {
  val man1 = new ManualTaskDefinition(List(
    IntField("Simple Int field", "intfield")
  ))
  val man2 = new ManualTaskDefinition(List(
    StringField("Simple String field", "strfield")
  ))
  val branch = new BranchTaskDefinition(context => {
    context.task.parent.get.value.get[Int]("intfield").get > 100
  })
  val subflow = new SubWorkflowTaskDefinition(Demo)
  val join1 = new JoinTaskDefinition(Set(subflow, branch), true)
  val join2 = new JoinTaskDefinition(Set(join1, man2))

  override val transitions: Map[(TaskDefinition, ActionResult), List[TaskDefinition]] = Map(
    (StartTaskDefinition, Ok) -> List(man1, man2),
    (man1, Ok) -> List(branch),
    (branch, Yes) -> List(subflow),
    (branch, No) -> List(join1),
    (subflow, Ok) -> List(join1),
    (join1, Ok) -> List(join2),
    (man2, Ok) -> List(join2),
    (join2, Ok) -> List(EndTaskDefinition)
  )
  override val name: String = "Demo"
}
