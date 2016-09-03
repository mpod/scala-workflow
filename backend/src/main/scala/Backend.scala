import actors.{IdAllocatorActor, MockupActor}
import actors.PrivateActorMessages.IdAllocatorActorRef
import akka.actor.{ActorSystem, Props}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


object Backend extends App {
  val system = ActorSystem("workflows")
  val idAllocator = system.actorOf(Props[IdAllocatorActor], "allocator")
  val router = system.actorOf(Props[actors.RouterActor], "router")

  router ! IdAllocatorActorRef(idAllocator)

  val mockupActor = system.actorOf(Props[MockupActor], "mockup")

  Await.result(system.whenTerminated, Duration.Inf)
}
