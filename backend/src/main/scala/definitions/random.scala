package definitions

import engine._
import util.Random

object RandomIf extends TaskDefinition {
  override def action(context: TaskActionContext): Option[ActionResult] = if (new Random().nextBoolean) Option(Yes) else Option(No)
  override def name: String = "If"
}

object RandomTaskA extends TaskDefinition {
  override def action(context: TaskActionContext): Option[ActionResult] = if (new Random().nextBoolean) Option(Ok) else None
  override def name: String = "TaskA"
}

object RandomTaskB extends TaskDefinition {
  override def action(context: TaskActionContext): Option[ActionResult] = if (new Random().nextBoolean) Option(Ok) else None
  override def name: String = "TaskB"
}

object RandomWorkflow extends WorkflowDefinition {
  override val transitions: Map[(TaskDefinition, ActionResult), List[TaskDefinition]] = Map(
    (StartTaskDefinition, Ok) -> List(RandomIf),
    (RandomIf, Yes) -> List(RandomTaskA),
    (RandomIf, No) -> List(RandomTaskB),
    (RandomTaskA, Ok) -> List(EndTaskDefinition),
    (RandomTaskB, Ok) -> List(EndTaskDefinition)
  )
  override val name: String = "Random workflow"
}
