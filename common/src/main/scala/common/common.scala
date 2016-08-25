package common

import spray.json._

sealed abstract class ManualTaskFieldViewBase
case class ManualTaskStringFieldView(name: String, label: String, value: Option[String], typeName: String) extends ManualTaskFieldViewBase
case class ManualTaskIntFieldView(name: String, label: String, value: Option[Int], typeName: String) extends ManualTaskFieldViewBase

sealed abstract class TaskViewBase
case class TaskView(id: Int, state: String, defName: String) extends TaskViewBase
case class ManualTaskView[+T <: ManualTaskFieldViewBase](id: Int, state:String, defName: String, fields: Seq[T]) extends TaskViewBase

case class WorkflowView[+T <: TaskViewBase](id: Int, tasks: Map[Int, T])

object WorkflowJsonProtocol extends DefaultJsonProtocol {
  implicit val manualTaskStringFieldViewJsonFormat = jsonFormat4(ManualTaskStringFieldView)
  implicit val manualTaskIntFieldViewJsonFormat = jsonFormat4(ManualTaskIntFieldView)
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
  implicit val workflowViewJsonFormat = new RootJsonFormat[WorkflowView[_]] {
    def write(wf: WorkflowView[_]) = JsObject(
      "id" -> JsNumber(wf.id),
      "tasks" -> JsArray(wf.tasks.values.map({
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

object PublicActorMessages {
  case object GetWorkflows
  case class CreateWorkflow(wfDefName: String)
  case class CreatedWorkflow(wfDefName: String, id: Int)
  case class Workflows(wfViews: Seq[WorkflowView[_]])
}