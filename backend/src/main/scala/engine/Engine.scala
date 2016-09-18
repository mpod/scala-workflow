package engine

import definitions.{Demo, ExampleWorkflow, RandomWorkflow}

class Engine(idGen: IdGenerator) {

  private var _workflows: List[Workflow] = List.empty[Workflow]

  implicit private val _engine: Engine = this

  def workflowDefinitions: Seq[WorkflowDefinition] = List(RandomWorkflow, ExampleWorkflow, Demo)

  def workflows: Seq[Workflow] = _workflows filter {_.parentWorkflow.isEmpty}

  def idGenerator: IdGenerator = idGen

  def startWorkflow(wfDef: WorkflowDefinition, label: String): Workflow = {
    startWorkflow(wfDef, label, None)
  }

  def startWorkflow(wfDef: WorkflowDefinition, label: String, parentWf: Workflow): Workflow = {
    startWorkflow(wfDef, label, Some(parentWf))
  }

  def startWorkflow(wfDef: WorkflowDefinition, label: String, parentWf: Option[Workflow]): Workflow = {
    val wf = new Workflow(idGenerator.nextId, wfDef, label, parentWf)
    wf.start
    _workflows ::= wf
    wf
  }

  def executeRound: Seq[Workflow] = for {
    wf <- _workflows
    if !wf.allExecuted
  } yield {
    wf.executeRound
    wf
  }

  def findWorkflow(wfId: Int): Option[Workflow] = workflows find (_.id == wfId)

  def setManualTaskFields(wfId: Int, taskId: Int, values: Map[String, String]): Unit =
    findWorkflow(wfId) match {
      case Some(wf) => wf.setManualTaskFields(taskId, values)
      case None => throw new IllegalArgumentException("Workflow not found.")
    }

  def getManualTaskFields(wfId: Int, taskId: Int): Seq[ManualTaskDefinition.Field] =
    findWorkflow(wfId) match {
      case Some(wf) => wf.getManualTaskFields(taskId)
      case None => throw new IllegalArgumentException("Workflow not found.")
    }
}

