package engine

import engine.ActionResult.{JoinIsWaiting, No, Ok, Yes}
import engine.Task.TaskContext

abstract class TaskDefinition {

  def action(implicit context: TaskContext): Option[ActionResult]

  def name: String
}

object TaskDefinition {

  class ProcessTaskDefinition(func: (TaskContext) => Unit) extends TaskDefinition {

    override def action(implicit context: TaskContext): Option[ActionResult] = {
      func(context)
      Some(Ok)
    }

    override def name: String = "Process"

  }

  class SubWorkflowTaskDefinition(wfDef: WorkflowDefinition) extends TaskDefinition {

    private val key = "SubflowTaskDefinition_%d".format(this.hashCode())

    def subWorkflow(implicit context: TaskContext): Option[Workflow] = context.task.get[Workflow](key)

    override def action(implicit context: TaskContext): Option[ActionResult] ={
      subWorkflow orElse {
        val wf: Workflow = context.engine.startWorkflow(wfDef, "SubWorkflow", context.task.workflow)
        context.task.put(key, wf)
        None
      } flatMap {
        wf => if (wf.endExecuted) Some(Ok) else None
      }
    }

    override def name: String = "SubWorkflow[%s]".format(wfDef.name)

  }

  class BranchTaskDefinition(func: (TaskContext) => Boolean) extends TaskDefinition {

    override def action(implicit context: TaskContext): Option[ActionResult] =
      if (func(context))
        Some(Yes)
      else
        Some(No)

    override def name: String = "Branch"

  }

  class SplitTaskDefinition extends TaskDefinition {

    override def action(implicit context: TaskContext): Option[ActionResult] = Some(Ok)

    override def name: String = "Split"

  }

  class JoinTaskDefinition(val waitFor: Set[TaskDefinition]) extends TaskDefinition {

    private val key = "JoinTaskDefinition_%d".format(this.hashCode())

    override def action(implicit context: TaskContext): Option[ActionResult] = {
      val parentDef: TaskDefinition = context.task.parent.get.value.taskDef
      val parents = context.workflow.get[Set[TaskDefinition]](key).map(_ + parentDef).getOrElse(Set(parentDef))

      context.workflow.put(key, parents)
      if (waitFor == parents)
        Some(Ok)
      else
        Some(JoinIsWaiting)
    }

    override def name: String = "Join"

  }

  object StartTaskDefinition extends TaskDefinition {

    override def action(implicit context: TaskContext): Option[ActionResult] = Option(Ok)

    override def name: String = "Start"

  }

  object EndTaskDefinition extends TaskDefinition {

    override def action(implicit context: TaskContext): Option[ActionResult] = Option(Ok)

    override def name: String = "End"

  }

}
