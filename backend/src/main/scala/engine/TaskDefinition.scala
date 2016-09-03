package engine

import engine.ActionResult.{JoinIsWaiting, No, Ok, Yes}
import engine.Task.TaskActionContext

abstract class TaskDefinition {
  def action(context: TaskActionContext): Option[ActionResult]
  def name: String
}

object TaskDefinition {

  class ProcessTaskDefinition(func: (TaskActionContext) => Unit) extends TaskDefinition {
    override def action(context: TaskActionContext): Option[ActionResult] = {
      func(context)
      Some(Ok)
    }

    override def name: String = "Process"
  }

  class SubWorkflowTaskDefinition(wfDef: WorkflowDefinition) extends TaskDefinition {
    var wf: Option[Workflow] = None

    override def action(context: TaskActionContext): Option[ActionResult] = wf match {
      case None => wf = Some(context.engine.startWorkflow(wfDef, context.task.workflow)); None
      case Some(x) => if (x.endExecuted) Some(Ok) else None
    }

    override def name: String = "SubWorkflow[%s]".format(wfDef.name)
  }

  class BranchTaskDefinition(f: (TaskActionContext) => Boolean) extends TaskDefinition {
    override def action(context: TaskActionContext): Option[ActionResult] = if (f(context)) Some(Yes) else Some(No)

    override def name: String = "Branch"
  }

  class SplitTaskDefinition extends TaskDefinition {
    override def action(context: TaskActionContext): Option[ActionResult] = Some(Ok)

    override def name: String = "Split"
  }

  object StartTaskDefinition extends TaskDefinition {
    override def action(context: TaskActionContext): Option[ActionResult] = Option(Ok)

    override def name: String = "Start"
  }

  object EndTaskDefinition extends TaskDefinition {
    override def action(context: TaskActionContext): Option[ActionResult] = Option(Ok)

    override def name: String = "End"
  }

  class JoinTaskDefinition(n: Int) extends TaskDefinition {
    private var inputLines = n

    override def action(context: TaskActionContext): Option[ActionResult] = {
      inputLines -= 1
      if (inputLines == 0)
        Some(Ok)
      else
        Some(JoinIsWaiting)
    }

    override def name: String = "Join"
  }

}
