package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import play.api.mvc._
import akka.pattern.ask
import akka.util.Timeout
import common.PublicActorMessages.{Error, GetWorkflows, Workflows}
import spray.json._
import common.Views.ViewsJsonProtocol._
import common.Views.{ManualTaskView, TaskViewBase, WorkflowView}

import scala.concurrent.duration._
import scala.language.postfixOps

class Application @Inject() (webJarAssets: WebJarAssets, system: ActorSystem)  extends Controller {
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(10 seconds)
  val actorPath = "akka.tcp://workflows@127.0.0.1:2662/user/mockup"

  def index = Action.async { implicit request =>
    (system.actorSelection(actorPath) ? GetWorkflows).mapTo[Workflows[_]].map({
      case Workflows(workflows) => Ok(views.html.index(workflows, webJarAssets))
    })
  }

  def workflow(wfId: Int) = Action.async { implicit request =>
    (system.actorSelection(actorPath) ? GetWorkflows).mapTo[Workflows[_]].map({
      case Workflows(workflows) =>
        workflows.find(wf => wf.id == wfId) match {
          case Some(wf: WorkflowView[_]) => Ok(views.html.workflow(wf, webJarAssets))
          case None => InternalServerError("Workflow not found.")
        }
    })
  }

  def task(wfId: Int, taskId: Int) = Action.async { implicit request =>
    (system.actorSelection(actorPath) ? GetWorkflows).mapTo[Workflows[TaskViewBase]].map({
      case Workflows(workflows) =>
        val wf = workflows find {_.id == wfId}
        val task = wf flatMap {_.tasks find {_.id == taskId}}

        task match {
          case Some(t: ManualTaskView[_]) => Ok(views.html.task(wf.get, t, webJarAssets))
          case None => InternalServerError("Task not found.")
        }
    })
  }

  def json = Action.async {
    (system.actorSelection(actorPath) ? GetWorkflows).mapTo[Workflows[_]].map({
      case Workflows(workflows) => Ok(workflows.toJson.toString)
    })
  }
}
