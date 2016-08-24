package controllers

import javax.inject.Inject

import akka.actor.{Actor, ActorIdentity, ActorSystem, Identify, Props, ReceiveTimeout}
import play.api.mvc._
import akka.pattern.ask
import akka.util.Timeout
import common.Messages.Message
import scala.concurrent.duration._
import scala.language.postfixOps

class Application @Inject() (webJarAssets: WebJarAssets, system: ActorSystem)  extends Controller {
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(10 seconds)

  def indexAsync = Action.async {
    val path = "akka.tcp://workflows@127.0.0.1:2662/user/simple"
    (system.actorSelection(path) ? Message("RTYZ")).mapTo[Message].map({
      case Message(msg) => Ok(msg)
    })
    //Ok(views.html.index(webJarAssets))
  }

  var index = Action {
    Ok(views.html.index(webJarAssets))
  }

  def workflow(wfId: Int) = Action { implicit request =>
    Ok(views.html.workflow(wfId, webJarAssets))
  }

  def task(wfId: Int, taskId: Int) = Action { implicit request =>
    Ok(views.html.task(wfId, taskId, webJarAssets))
  }
}
