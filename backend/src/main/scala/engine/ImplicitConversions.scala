package engine

import scala.language.implicitConversions
import common.Views._
import engine.Task.TaskActionContext

object ImplicitConversions {

  implicit def toWorkflowViewSeq(wfSeq: Seq[Workflow]): Seq[WorkflowView] = wfSeq map toWorkflowView

  implicit def toWorkflowView(wf: Workflow): WorkflowView =
    WorkflowView(wf.id, wf.workflowDef.name, "Label", "State", wf.tasks map toTaskView)

  implicit def toTaskView(task: Task): TaskViewBase = task.taskDef match {
    case taskDef: ManualTaskDefinition =>
      implicit val context = task.context
      ManualTaskView(task.id, "TaskState", task.taskDef.name, taskDef.fields map toFieldView)
    case _ => TaskView(task.id, "TaskState", task.taskDef.name)
  }

  implicit def toFieldView(field: ManualTaskDefinition.Field)(implicit context: TaskActionContext): ManualTaskFieldViewBase = field match {
    case f: ManualTaskDefinition.IntField => ManualTaskIntFieldView(f.name, f.label, f.value)
    case f: ManualTaskDefinition.StringField => ManualTaskStringFieldView(f.name, f.label, f.value)
    case _ => throw new UnsupportedOperationException("Unsupported field type.")
  }

}

