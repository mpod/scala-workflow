package actors

import actors.IdAllocatorActor.{AllocateIdBlock, AllocatedIdBlock}
import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.routing.ConsistentHashingRouter.ConsistentHashMapping
import akka.routing._
import akka.util.Timeout
import engine._
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.language.postfixOps

case object GetWorkflows
case class CreateWorkflow(wfDef: WorkflowDefinition)
case class CreateWorkflowExtended(wfDef: WorkflowDefinition, id: Int)
case class IdAllocatorActorRef(ref: ActorRef)

trait IdConsumer {
  var idAllocator: ActorRef
  var allocatedIds: List[Int]

  def consumeNextId: Int = {
    if (allocatedIds.isEmpty)
      throw new RuntimeException("System under too big load!")
    val id = allocatedIds.head
    allocatedIds = allocatedIds.tail
    if (allocatedIds.length < 10) idAllocator ! AllocateIdBlock
    id
  }

  def extendAvailableIds(newIds: List[Int]): Unit = {
    allocatedIds ++= newIds
  }
}

class RouterActor extends Actor with IdConsumer {
  import context._

  var idAllocator: ActorRef = ActorRef.noSender
  var allocatedIds: List[Int] = List.empty
  implicit val timeout = Timeout(10 seconds)

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
      val senderRef = sender()
      val f = Future.sequence(context.children map {c => (c ? GetWorkflows).mapTo[String]})
      f onSuccess {
        case workflows =>
          senderRef ! workflows
      }
    case AllocatedIdBlock(ids) =>
      extendAvailableIds(ids)
    case CreateWorkflow(wfDef) =>
      router.route(CreateWorkflowExtended(wfDef, consumeNextId), sender())
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

class EngineActor extends Actor with IdConsumer {
  val engine = new Engine()
  var idAllocator: ActorRef = ActorRef.noSender
  var allocatedIds: List[Int] = List.empty

  def receive = {
    case CreateWorkflowExtended(wfDef, id) =>
      sender() ! s"Created workflow $id"
    case IdAllocatorActorRef(ref) =>
      idAllocator = ref
    case AllocatedIdBlock(ids) =>
      extendAvailableIds(ids)
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
