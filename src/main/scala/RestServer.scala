import actors.{IdAllocatorActor, IdAllocatorActorRef}
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.duration._
import scala.io.StdIn
import scala.language.postfixOps
import scala.util.Random

object RestServer {
  def main(args: Array[String]) {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    implicit val timeout = Timeout(10 seconds)

    val idAllocator = system.actorOf(Props[IdAllocatorActor])
    val router = system.actorOf(Props[actors.RouterActor], "router")

    router ! IdAllocatorActorRef(idAllocator)

    val route =
      pathPrefix("workflows") {
        pathEnd {
          get {
            val response = (router ? Random.nextInt(15)).mapTo[String]
            complete(response)
          } ~
          post {
            complete("create workflow")
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
