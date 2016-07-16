import actors._
import akka.actor.{ActorSystem, Inbox, Props}
import definitions.ExampleWorkflow
import scala.concurrent.duration._

object ActorApp {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("Workflows")

    val engineActor = system.actorOf(Props[EngineActor], name = "Engine")

    val inbox = Inbox.create(system)

    engineActor ! StartWorkflow(ExampleWorkflow)

  }
}