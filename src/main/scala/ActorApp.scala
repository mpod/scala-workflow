import actors._
import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import definitions.ExampleWorkflow

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object ActorApp {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("Workflows")

    val engineActor = system.actorOf(Props[EngineActor], name = "Engine")

    implicit val timeout = Timeout(5 seconds)
    val future = engineActor ? StartWorkflow(ExampleWorkflow)
    Await.result(future, timeout.duration).asInstanceOf[String]

    def executeRound() {
      val future = engineActor ? ExecuteRound()
      Await.result(future, timeout.duration) match {
        case AllWorkflowsFinished() => println("All workflows finished")
        case SomeWorkflowsUpdated() => executeRound()
      }

    }
    executeRound()

    system.stop(engineActor)
    system.terminate()
  }
}