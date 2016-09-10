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
import common.Views.{ManualTaskView, TaskViewBase, WorkflowView}
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
        workflows.find(wf => wf.id == wfId) match {
          case Some(wf: WorkflowView) => Ok(views.html.workflow(wf, webJarAssets))
          case None => InternalServerError("Workflow not found.")
        }
    })
  }

  def task(wfId: Int, taskId: Int) = Action.async { implicit request =>
    (system.actorSelection(actorPath) ? GetWorkflows).mapTo[Workflows].map({
      case Workflows(workflows) =>
        val wf = workflows find {_.id == wfId}
        val task = wf flatMap {_.tasks find {_.id == taskId}}

        task match {
          case Some(t: ManualTaskView) => Ok(views.html.task(wf.get, t, t.state == "Done", webJarAssets))
          case None => InternalServerError("Task not found.")
        }
    })
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

  def executeManualTask(wfId: Int, taskId: Int) = Action.async { implicit request =>
    def findTask(workflows: Seq[WorkflowView], wfId: Int, taskId: Int): TaskViewBase = {
      workflows find {_.id == wfId} flatMap {_.tasks find {_.id == taskId}} get
    }
    val actor = system.actorSelection(actorPath)

    (actor ? GetWorkflows).map({
      case Workflows(workflows) => findTask(workflows, wfId, taskId)
    }).flatMap({
      case t: ManualTaskView =>
        val fieldValues = request.body.asFormUrlEncoded.get.map({case (k, v) => (k, v.head)})
        actor ? ExecuteManualTask(wfId, taskId, fieldValues)
    }).map({
      case Workflows(_) => Redirect(routes.Application.workflow(wfId)).flashing("success" -> "Task executed successfully!")
      case Error(message) => Redirect(routes.Application.task(wfId, taskId)).flashing("error" -> s"An exception occurred: $message")
    }).fallbackTo(Future{
      Redirect(routes.Application.workflow(wfId)).flashing("error" -> "Exception while executing a task.")
    })
  }

  def json = Action.async {
    (system.actorSelection(actorPath) ? GetWorkflows).mapTo[Workflows].map({
      case Workflows(workflows) => Ok(workflows.toJson.toString)
    })
  }
}
