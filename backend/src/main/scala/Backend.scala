import akka.actor.{Actor, ActorSystem, Props}
import common.Messages.Message

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class SimpleActor extends Actor {
  def receive = {
    case Message(msg) =>
      println(msg)
      sender() ! Message(msg)
  }
}

object Backend extends App {
  val system = ActorSystem("workflows")

  val simpleActor = system.actorOf(Props[SimpleActor], "simple")

  Await.result(system.whenTerminated, Duration.Inf)
}
