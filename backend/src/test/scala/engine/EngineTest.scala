package engine

import org.scalatest.FlatSpec
import Fixtures._

class EngineTest extends FlatSpec {

  behavior of "An Engine"

  it should "start a workflow" in {
    val f = emptyWorkflow

    assert(f.engine.workflows.isEmpty)
    val wf = f.engine.startWorkflow(f.wfDef, "Test")
    assert(wf.isInstanceOf[Workflow])
    assert(f.engine.workflows.length == 1)
    assert(f.engine.findWorkflow(wf.id).get == wf)
  }

  it should "execute a workflow" in {
    val f = emptyWorkflow

    f.engine.startWorkflow(f.wfDef, "Test")
    var updated = f.engine.executeRound
    assert(updated.length == 1)
    updated = f.engine.executeRound
    assert(updated.length == 1)
    updated = f.engine.executeRound
    assert(updated.isEmpty)
  }

}
