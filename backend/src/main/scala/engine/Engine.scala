package engine

import definitions.{ExampleWorkflow, RandomWorkflow}
import engine.Task.TaskContext

class Engine(implicit idGen: IdGenerator) {
  private var _workflows = List.empty[Workflow]

  def workflowDefinitions: Seq[WorkflowDefinition] = List(RandomWorkflow, ExampleWorkflow)

  def workflows = _workflows filter {_.parentWorkflow.isEmpty}

  def startWorkflow(wfDef: WorkflowDefinition, label: String): Workflow = {
    startWorkflow(wfDef, label, None)
  }

  def startWorkflow(wfDef: WorkflowDefinition, label: String, parentWf: Workflow): Workflow = {
    startWorkflow(wfDef, label, Some(parentWf))
  }

  def startWorkflow(wfDef: WorkflowDefinition, label: String, parentWf: Option[Workflow]): Workflow = {
    val wf = new Workflow(wfDef, label, parentWf, this)
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

  def findWorkflow(wfId: Int) = _workflows.find(_.id == wfId)

  private def findManualTask(wfId: Int, taskId: Int): Option[Task] = for {
    wf <- findWorkflow(wfId)
    task <- wf.findTask(taskId)
    if task.taskDef.isInstanceOf[ManualTaskDefinition]
  } yield task

  def setManualTaskFields(wfId: Int, taskId: Int, values: Map[String, String]): Unit = {
    findManualTask(wfId, taskId) match {
      case Some(t) =>
        val taskDef = t.taskDef
        val context = t.context
        taskDef match {
          case td: ManualTaskDefinition => values.foreach({case (k, v) => td.setField(k, v)(context)})
          case _ => throw new UnknownError("Unexpected error.")
        }
      case None => throw new IllegalArgumentException("Manual task not found.")
    }
  }

  def getManualTaskFields(wfId: Int, taskId: Int): Seq[ManualTaskDefinition.Field] = {
    findManualTask(wfId, taskId) match {
      case Some(t) => t.taskDef match {
        case td: ManualTaskDefinition => td.fieldsMap.values.toSeq
        case _ => throw new UnknownError("Unexpected error.")
      }
      case None => throw new IllegalArgumentException("Manual task not found.")
    }
  }
}

