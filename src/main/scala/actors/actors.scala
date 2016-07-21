package actors

import actors.IdAllocatorActor.{AllocateBlock, AllocatedBlock}
import akka.actor.{Actor, ActorRef, Props}
import akka.routing.ConsistentHashingRouter.ConsistentHashMapping
import akka.routing._
import engine._

case object GetWorkflows
case class CreateWorkflow(wfDef: WorkflowDefinition)
case class IdAllocatorActorRef(ref: ActorRef)

class RouterActor extends Actor {
  var idAllocator: ActorRef = ActorRef.noSender

  def hashMapping: ConsistentHashMapping = {
    case wfId: Int => wfId
  }

  var router = {
    val routees = new Range(1, 11, 1).map(i => {
      val r = context.actorOf(ViewActor.props(i))
      context watch r
      ActorRefRoutee(r)
    })
    Router(ConsistentHashingRoutingLogic(context.system, 10, hashMapping = hashMapping), routees)
  }

  def receive = {
    case IdAllocatorActorRef(ref) =>
      context.children.foreach(_ ! IdAllocatorActorRef(ref))
      idAllocator = ref
      idAllocator ! AllocateBlock
    case GetWorkflows => ???
    case AllocatedBlock(identifiers) =>
      println(identifiers)
  }
}

object ViewActor {
  def props(index: Int): Props = Props(new ViewActor(index))

}

class ViewActor(index: Int) extends Actor {
  val engineChild = context.actorOf(Props[EngineActor])

  def receive = {
    case GetWorkflows =>
      sender() ! "List of workflows"
    case CreateWorkflow =>
      engineChild ! CreateWorkflow
    case IdAllocatorActorRef(ref) => engineChild ! IdAllocatorActorRef(ref)
  }
}

class EngineActor extends Actor {
  val engine = new Engine()
  var idAllocator: ActorRef = ActorRef.noSender

  def receive = {
    case CreateWorkflow(wfDef) =>
      engine.startWorkflow(wfDef)
    case IdAllocatorActorRef(ref) =>
      idAllocator = ref
  }
}

object IdAllocatorActor {
  case object AllocateBlock
  case class AllocatedBlock(identifiers: IndexedSeq[Int])
}

class IdAllocatorActor extends Actor {
  var lastId = 0
  val blockSize = 50

  def receive = {
    case AllocateBlock =>
      sender() ! AllocatedBlock(new Range(lastId, lastId + blockSize, 1))
      lastId += blockSize
  }
}
