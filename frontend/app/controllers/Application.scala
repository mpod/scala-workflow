package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import play.api.mvc._
import play.api.data.Forms._
import akka.pattern.ask
import akka.util.Timeout
import spray.json._
import common.PublicActorMessages._
import common.Views.ViewsJsonProtocol._
import common.Views.{ManualTaskView, SubWorkflowTaskView, TaskViewBase, WorkflowView}
import play.api.data.Form

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class Application @Inject() (webJarAssets: WebJarAssets, system: ActorSystem)  extends Controller {
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(10 seconds)
  val actorPath = "akka.tcp://workflows@127.0.0.1:2662/user/router"
  //val actorPath = "akka.tcp://workflows@127.0.0.1:2662/user/mockup"
  val wfForm = Form(mapping(
    "name" -> nonEmptyText,
    "label" -> nonEmptyText
  )(StartWorkflow.apply)(StartWorkflow.unapply))

  def index = Action.async { implicit request =>
    val actorRef = system.actorSelection(actorPath)
    for {
      wfs <- (actorRef ? GetWorkflows).mapTo[Workflows]
      wfDefs <- (actorRef ? GetWorkflowDefinitions).mapTo[WorkflowDefinitions]
    } yield Ok(views.html.index(wfs.wfViews, wfDefs.wfDefNames, webJarAssets))
  }

  def workflow(wfId: Int) = Action.async { implicit request =>
    val actorRef = system.actorSelection(actorPath)
    (actorRef ? GetWorkflows).mapTo[Workflows].map({
      case Workflows(workflows) =>
        val wfPath = findWorkflowPath(wfId, workflows.toList)
        wfPath match {
          case wf :: wfs => Ok(views.html.workflow(wfPath.last, wfPath.last.state == "Finished", webJarAssets))
          case List() => InternalServerError("Workflow not found.")
        }
    })
  }

  def task(wfRootId: Int, wfId: Int, taskId: Int) = Action.async { implicit request =>
    (system.actorSelection(actorPath) ? GetWorkflows).mapTo[Workflows].map({
      case Workflows(workflows) =>
        val wfPath = findWorkflowPath(wfId, workflows.toList)
        val task = wfPath.lastOption flatMap {_.tasks find {_.id == taskId}}

        task match {
          case Some(t: ManualTaskView) if wfPath.head.id == wfRootId =>
            Ok(views.html.task(wfPath.head, wfPath.last, t, t.state == "Done", webJarAssets))
          case _ =>
            InternalServerError("Task not found.")
        }
    })
  }

  def findWorkflowPath(wfId: Int, workflows: List[WorkflowView]): List[WorkflowView] = {
    (List.empty[WorkflowView] /: workflows) {(z, wf) =>
      if (wf.id == wfId)
        List(wf)
      else
        z match {
          case List() =>
            val subWorkflows = wf.tasks collect { case t: SubWorkflowTaskView if t.subwf.isDefined => t.subwf.get }
            val wfPath = findWorkflowPath(wfId, subWorkflows.toList)
            wfPath match {
              case List() =>
                List()
              case _ =>
                wf :: wfPath
            }
          case _ =>
            z
        }
    }
  }

  def createWorkflow() = Action.async { implicit request =>
    wfForm.bindFromRequest.fold(
      formWithErrors =>
        Future(Redirect(routes.Application.index()).flashing("error" -> "Failed workflow creation!")),
      value =>
        (system.actorSelection(actorPath) ? value).mapTo[Workflows].map({
          case Workflows(workflows) if workflows.length == 1 =>
            val wf = workflows.head
            Redirect(routes.Application.index()).flashing("success" -> "Created workflow '%s' with id=%d!".format(wf.label, wf.id))
        }).recover({
          case _: ClassCastException | _: MatchError =>
            Redirect(routes.Application.index()).flashing("error" -> "Exception in starting a workflow!")
        })
    )
  }

  def executeManualTask(wfRootId: Int, wfId: Int, taskId: Int) = Action.async { implicit request =>
    val actor = system.actorSelection(actorPath)
    val fieldValues = request.body.asFormUrlEncoded.get.map({ case (k, v) => (k, v.head) })

    (actor ? ExecuteManualTask(wfRootId, wfId, taskId, fieldValues)).map({
      case Workflows(_) => Redirect(routes.Application.workflow(wfRootId)).flashing("success" -> "Task executed successfully!")
      case Error(message) => Redirect(routes.Application.task(wfRootId, wfId, taskId)).flashing("error" -> s"An exception occurred: $message")
    })
  }

  def json = Action.async {
    (system.actorSelection(actorPath) ? GetWorkflows).mapTo[Workflows].map({
      case Workflows(workflows) => Ok(workflows.toJson.toString)
    })
  }
}
