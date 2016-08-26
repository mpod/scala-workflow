package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import play.api.mvc._
import akka.pattern.ask
import akka.util.Timeout
import common.PublicActorMessages.{GetWorkflows, Workflows}
import spray.json._
import common.Views.ViewsJsonProtocol._

import scala.concurrent.duration._
import scala.language.postfixOps

class Application @Inject() (webJarAssets: WebJarAssets, system: ActorSystem)  extends Controller {
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(10 seconds)

  def index = Action.async {
    val path = "akka.tcp://workflows@127.0.0.1:2662/user/mockup"
    (system.actorSelection(path) ? GetWorkflows).mapTo[Workflows].map({
      case Workflows(workflows) => Ok(workflows.toJson.prettyPrint)
    })
    //Ok(views.html.index(webJarAssets))
  }

  def workflow(wfId: Int) = Action { implicit request =>
    Ok(views.html.workflow(wfId, webJarAssets))
  }

  def task(wfId: Int, taskId: Int) = Action { implicit request =>
    Ok(views.html.task(wfId, taskId, webJarAssets))
  }
}
