package definitions

import engine._
import util.Random

object RandomStart extends TaskDefinition {
  override def action: Option[ActionResult] = Option(Ok)
  override def name: String = "Start"
}

object RandomEnd extends TaskDefinition {
  override def action: Option[ActionResult] = Option(Ok)
  override def name: String = "End"
}

object RandomIf extends TaskDefinition {
  override def action: Option[ActionResult] = if (new Random().nextBoolean) Option(Yes) else Option(No)
  override def name: String = "If"
}

object RandomTaskA extends TaskDefinition {
  override def action: Option[ActionResult] = if (new Random().nextBoolean) Option(Ok) else None
  override def name: String = "TaskA"
}

object RandomTaskB extends TaskDefinition {
  override def action: Option[ActionResult] = if (new Random().nextBoolean) Option(Ok) else None
  override def name: String = "TaskB"
}

object RandomWorkflow extends WorkflowDefinition {
  override val taskDefinitions: List[TaskDefinition] = List(
    RandomStart, RandomEnd, RandomIf, RandomTaskA, RandomTaskB
  )
  override val start: TaskDefinition = RandomStart
  override val end: List[TaskDefinition] = List(RandomEnd)
  override val transitions: Map[(TaskDefinition, ActionResult), List[TaskDefinition]] = Map(
    (RandomStart, Ok) -> List(RandomIf),
    (RandomIf, Yes) -> List(RandomTaskA),
    (RandomIf, No) -> List(RandomTaskB),
    (RandomTaskA, Ok) -> List(RandomEnd),
    (RandomTaskB, Ok) -> List(RandomEnd)
  )
}
