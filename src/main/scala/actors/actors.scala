package actors

import actors.IdAllocatorActor.{AllocateIdBlock, AllocatedIdBlock}
import akka.actor.{Actor, ActorRef, Props}
import akka.routing.ConsistentHashingRouter.ConsistentHashMapping
import akka.routing._
import engine._

case object GetWorkflows
case class CreateWorkflow(wfDef: WorkflowDefinition)
case class CreateWorkflowExtended(wfDef: WorkflowDefinition, id: Int)
case class IdAllocatorActorRef(ref: ActorRef)

class RouterActor extends Actor {
  import context._

  var idAllocator: ActorRef = ActorRef.noSender
  var allocatedIds: List[Int] = List.empty

  def hashMapping: ConsistentHashMapping = {
    case CreateWorkflowExtended(wfDef, id) => id
  }

  var router = {
    val routees = new Range(1, 11, 1).map(i => {
      val r = context.actorOf(ViewActor.props(i))
      context watch r
      ActorRefRoutee(r)
    })
    Router(ConsistentHashingRoutingLogic(context.system, 10, hashMapping = hashMapping), routees)
  }

  def receive = uninitialized

  def uninitialized: Receive = {
    case IdAllocatorActorRef(ref) =>
      context.children.foreach(_ ! IdAllocatorActorRef(ref))
      idAllocator = ref
      idAllocator ! AllocateIdBlock
      become(initialized)
    case _ =>
      throw new RuntimeException("Not ready!")
  }

  def initialized: Receive = {
    case GetWorkflows =>
    case AllocatedIdBlock(ids) =>
      allocatedIds ++= ids
    case CreateWorkflow(wfDef) =>
      if (allocatedIds.nonEmpty) {
        val id = allocatedIds.head
        allocatedIds = allocatedIds.tail
        if (allocatedIds.length < 10) idAllocator ! AllocateIdBlock
        router.route(CreateWorkflowExtended(wfDef, id), sender())
      } else
        throw new RuntimeException("System under too big load!")
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
    case msg: CreateWorkflowExtended =>
      engineChild forward msg
    case msg: IdAllocatorActorRef =>
      engineChild ! msg
  }
}

class EngineActor extends Actor {
  val engine = new Engine()
  var idAllocator: ActorRef = ActorRef.noSender
  var allocatedIds: List[Int] = List.empty

  def receive = {
    case CreateWorkflowExtended(wfDef, id) =>
      sender() ! s"Created workflow $id"
    case IdAllocatorActorRef(ref) =>
      idAllocator = ref
    case AllocatedIdBlock(ids) =>
      allocatedIds ++= ids
  }
}

object IdAllocatorActor {
  case object AllocateIdBlock
  case class AllocatedIdBlock(identifiers: List[Int])
}

class IdAllocatorActor extends Actor {
  var lastId = 0
  val blockSize = 50

  def receive = {
    case AllocateIdBlock =>
      sender() ! AllocatedIdBlock(new Range(lastId, lastId + blockSize, 1).toList)
      lastId += blockSize
  }
}
