package engine

import engine.ActionResult.Ok
import engine.IdGenerator.SimpleIdGenerator
import engine.TaskDefinition.{EndTaskDefinition, StartTaskDefinition}

object Fixtures {

  def emptyWorkflow =
    new {
      val wfDef = new WorkflowDefinition {
        override val transitions: Map[(TaskDefinition, ActionResult), List[TaskDefinition]] = Map(
          (StartTaskDefinition, Ok) -> List(EndTaskDefinition)
        )
        override val name: String = "Empty workflow"
      }
      val engine = new Engine(SimpleIdGenerator)
    }

}
