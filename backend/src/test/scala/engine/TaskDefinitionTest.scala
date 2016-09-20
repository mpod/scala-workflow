package engine

import org.scalatest.FlatSpec
import Fixtures._

class TaskDefinitionTest extends FlatSpec {

  "StartTaskDefinition" should "have name Start" in {
    val f = emptyWorkflow
    val wf = f.engine.startWorkflow(f.wfDef, "Test")
    assert(wf.tasks.head.taskDef.name == "Start")
  }

  "EndTaskDefinition" should "have name End" in {
    val f = emptyWorkflow
    val wf = f.engine.startWorkflow(f.wfDef, "Test")
    f.engine.executeRound
    f.engine.executeRound
    assert(wf.tasks.last.taskDef.name == "Start")
  }

}
