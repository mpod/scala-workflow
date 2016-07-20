package actors

import akka.actor.{Actor, Props}
import akka.routing.ConsistentHashingRouter.ConsistentHashMapping
import akka.routing._
import engine._

case class StartWorkflow(wfDef: WorkflowDefinition)
case class ExecuteRound()
case class AllWorkflowsFinished()
case class SomeWorkflowsUpdated()

class RouterActor extends Actor {
  def hashMapping: ConsistentHashMapping = {
    case wfId: Int => wfId
  }

  var router = context.system.actorOf(
    ConsistentHashingPool(10, hashMapping = hashMapping).props(Props[ViewActor]),
    name = "view"
  )

  def receive = {
    case a: String => router.route(a, sender())
  }
}

class ViewActor extends Actor {
  def receive = ???
}

class EngineActor extends Actor {
  val engine = new Engine()

  def receive = {
    case StartWorkflow(wfDef) =>
      engine.startWorkflow(wfDef)
    case ExecuteRound() =>
      engine.executeRound.nonEmpty
  }
}

object IdGenActor {
}

class IdGenActor extends Actor {
  def receive = ???
}
