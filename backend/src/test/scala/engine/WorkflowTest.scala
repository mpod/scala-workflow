package engine

import org.scalatest.FlatSpec
import Fixtures._
import engine.TaskDefinition.{EndTaskDefinition, StartTaskDefinition}

class WorkflowTest extends FlatSpec {

  behavior of "A Workflow"

  it should "create Start task" in {
    val f = emptyWorkflow
    val engine = f.engine

    val wf = new Workflow(engine.idGenerator.nextId, f.wfDef, "Test")
    val t = wf.start(engine)
    assert(t.taskDef == StartTaskDefinition)
  }

  it should "create End task" in {
    val f = emptyWorkflow
    val engine = f.engine

    val wf = new Workflow(engine.idGenerator.nextId, f.wfDef, "Test")
    wf.start(engine)
    val tasks = wf.executeRound(engine)
    val t = tasks.head
    assert(t.taskDef == EndTaskDefinition)
  }

}
