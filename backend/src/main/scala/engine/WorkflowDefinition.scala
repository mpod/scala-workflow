package engine

abstract class WorkflowDefinition {
  val transitions: Map[(TaskDefinition, ActionResult), List[TaskDefinition]]
  val name: String
}

