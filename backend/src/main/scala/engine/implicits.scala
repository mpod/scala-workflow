package engine

import scala.language.implicitConversions
import common.Views._

object Implicits {
  implicit def toWorkflowView(wf: Workflow): WorkflowView =
    WorkflowView(wf.id, wf.workflowDef.name, "Label", "State", wf.tasks map toTaskView)

  implicit def toTaskView(task: Task): TaskViewBase = task.taskDef match {
    case taskDef: ManualTaskDefinition => ManualTaskView(task.id, "TaskState", task.taskDef.name, taskDef.fields map toFieldView)
    case _ => TaskView(task.id, "TaskState", task.taskDef.name)
  }

  implicit def toFieldView(field: ManualTaskDefinition.Field): ManualTaskFieldViewBase = field.value match {
    case v: Option[Int] => ManualTaskIntFieldView(field.name, field.label, v)
    case v: Option[String] => ManualTaskStringFieldView(field.name, field.label, v)
    case _ => throw new UnsupportedOperationException("Unsupported field type.")
  }
}

