package common

import common.Views.{TaskViewBase, WorkflowView}
import spray.json._

object Views {
  sealed abstract class ManualTaskFieldViewBase {
    val name: String
    val label: String
    val value: Option[_]
    def valueToString: String = value match {
      case Some(v) => v.toString
      case None => ""
    }
  }
  case class ManualTaskStringFieldView(name: String, label: String, value: Option[String]) extends ManualTaskFieldViewBase
  case class ManualTaskIntFieldView(name: String, label: String, value: Option[Int]) extends ManualTaskFieldViewBase
  sealed abstract class TaskViewBase {
    val id: Int
    val state: String
    val defName: String
  }
  case class TaskView(id: Int, state: String, defName: String) extends TaskViewBase
  case class ManualTaskView[+T <: ManualTaskFieldViewBase](id: Int, state: String, defName: String, fields: Seq[T]) extends TaskViewBase

  case class WorkflowView[+T <: TaskViewBase](id: Int, name: String, state: String, tasks: Seq[T])

  object ViewsJsonProtocol extends DefaultJsonProtocol {
    implicit val manualTaskStringFieldViewJsonFormat = jsonFormat3(ManualTaskStringFieldView)
    implicit val manualTaskIntFieldViewJsonFormat = jsonFormat3(ManualTaskIntFieldView)
    implicit val manualTaskViewJsonFormat = new RootJsonFormat[ManualTaskView[_]] {
      def write(t: ManualTaskView[_]) = JsObject(
        "id" -> JsNumber(t.id),
        "state" -> JsString(t.state),
        "defName" -> JsString(t.defName),
        "fields" -> JsArray(t.fields.map({
          case f: ManualTaskIntFieldView => f.toJson
          case f: ManualTaskStringFieldView => f.toJson
          case _ => serializationError("Not supported.")
        }).toVector)
      )

      def read(value: JsValue) = value match {
        case _ => deserializationError("Not supported.")
      }
    }
    implicit val taskViewJsonFormat = jsonFormat3(TaskView)
    implicit def workflowViewJsonFormat[T <: TaskViewBase] = new RootJsonFormat[WorkflowView[T]] {
      def write(wf: WorkflowView[T]) = JsObject(
        "id" -> JsNumber(wf.id),
        "state" -> JsString(wf.state),
        "tasks" -> JsArray(wf.tasks.map({
          case t: TaskView => t.toJson
          case t: ManualTaskView[_] => manualTaskViewJsonFormat.write(t)
          case _ => serializationError("Not supported.")
        }).toVector)
      )

      def read(value: JsValue) = value match {
        case _ => deserializationError("Not supported.")
      }
    }
  }
}

object PublicActorMessages {
  case object GetWorkflows
  case class Workflows[+T <: TaskViewBase](wfViews: Seq[WorkflowView[T]])
  case class CreateWorkflow(wfDefName: String)
  case class StartedWorkflow(wfDefName: String, id: Int)
  case class Error(message: String)
}