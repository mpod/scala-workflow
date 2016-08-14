import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import akka.pattern.ask
import spray.json._
import actors.IdAllocatorActor
import actors.WorkflowProtocol.{CreateWorkflow, GetWorkflows, IdAllocatorActorRef}
import actors.WorkflowJsonProtocol._
import definitions.ExampleWorkflow
import engine.{TaskView, TaskViewBase, WorkflowView}
import scala.concurrent.duration._
import scala.io.StdIn
import scala.language.postfixOps

object RestServer {
  def main(args: Array[String]) {
    implicit val system = ActorSystem("workflows")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    implicit val timeout = Timeout(10 seconds)

    val idAllocator = system.actorOf(Props[IdAllocatorActor], "allocator")
    val router = system.actorOf(Props[actors.RouterActor], "router")

    router ! IdAllocatorActorRef(idAllocator)

    val route =
      pathPrefix("workflows") {
        pathEnd {
          get {
            onSuccess((router ? GetWorkflows).mapTo[Seq[WorkflowView[TaskView]]]) {
              workflowViews => {
                val json = JsArray(workflowViews.map(view => view.toJson).toVector)
                complete(json.toString)
              }
            }
          } ~
          post {
            onSuccess((router ? CreateWorkflow(ExampleWorkflow)).mapTo[String]) {
              response => {
                complete(response)
              }
            }
          }
        }
      } ~
      pathPrefix("workflow" / IntNumber) { wfId =>
        pathPrefix("task" / IntNumber) { taskId =>
          pathEnd {
            get {
              complete("AAA %d %d".format(wfId, taskId))
            }
          }
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}
