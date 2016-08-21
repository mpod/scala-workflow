package controllers

import javax.inject.Inject

import play.api.mvc._

class Application @Inject() (webJarAssets: WebJarAssets)  extends Controller {
  def index = Action { implicit request =>
    Ok(views.html.index(webJarAssets))
  }

  def workflow(wfId: Int) = Action { implicit request =>
    Ok(views.html.workflow(wfId, webJarAssets))
  }

  def task(wfId: Int, taskId: Int) = Action { implicit request =>
    Ok(views.html.task(wfId, taskId, webJarAssets))
  }
}
