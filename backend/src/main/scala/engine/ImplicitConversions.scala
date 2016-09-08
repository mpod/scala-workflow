package engine

import scala.language.implicitConversions
import common.Views._
import engine.Task.TaskContext

object ImplicitConversions {

  implicit def toWorkflowViewSeq(wfSeq: Seq[Workflow]): Seq[WorkflowView] = wfSeq map toWorkflowView

  implicit def toWorkflowView(wf: Workflow): WorkflowView = {
    def helper(z: String, t: Task): String = {
      if (z == "Waiting on manual task")
        z
      else if (!t.isExecuted && t.taskDef.isInstanceOf[ManualTaskDefinition])
        "Waiting on manual task"
      else if (!t.isExecuted)
        "Running"
      else
        z
    }
    val state = wf.tasks.foldLeft("Finished")(helper)
    WorkflowView(wf.id, wf.workflowDef.name, wf.label, state, wf.tasks map toTaskView)
  }

  implicit def toTaskView(task: Task): TaskViewBase = task.taskDef match {
    case taskDef: ManualTaskDefinition =>
      implicit val context = task.context
      ManualTaskView(task.id, "TaskState", task.taskDef.name, taskDef.fields map toFieldView)
    case _ => TaskView(task.id, "TaskState", task.taskDef.name)
  }

  implicit def toFieldView(field: ManualTaskDefinition.Field)(implicit context: TaskContext): ManualTaskFieldViewBase = field match {
    case f: ManualTaskDefinition.IntField => ManualTaskIntFieldView(f.name, f.label, f.value)
    case f: ManualTaskDefinition.StringField => ManualTaskStringFieldView(f.name, f.label, f.value)
    case _ => throw new UnsupportedOperationException("Unsupported field type.")
  }

}

