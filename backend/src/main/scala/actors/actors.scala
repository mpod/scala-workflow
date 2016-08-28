package actors

import actors.PrivateActorMessages._
import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.routing.ConsistentHashingRouter.ConsistentHashMapping
import akka.routing._
import akka.util.Timeout
import common.PublicActorMessages._
import common.Views.WorkflowView
import definitions.{ExampleWorkflow, RandomWorkflow}
import engine._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.language.implicitConversions
import scala.concurrent.ExecutionContext.Implicits.global

object PrivateActorMessages {
  case class IdAllocatorActorRef(ref: ActorRef)
  case class CreateWorkflowExtended(wfDefName: String, id: Int)
  case object ExecuteRound
  case object AllocateIdBlock
  case class AllocatedIdBlock(identifiers: Seq[Int])
}

class RouterActor extends Actor {
  implicit val timeout = Timeout(10 seconds)
  var idGenerator: ActorBasedIdGenerator = _

  def hashMapping: ConsistentHashMapping = {
    case CreateWorkflowExtended(wfDefName, id) => id
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
      idGenerator = new ActorBasedIdGenerator(ref)
      context.become(initialized)
    case _ =>
      throw new RuntimeException("Not ready!")
  }

  def initialized: Receive = {
    case GetWorkflows =>
      val senderRef = sender()
      val f = Future.sequence(context.children map {c => (c ? GetWorkflows).mapTo[Seq[WorkflowView]]})
      f onSuccess {
        case workflows => senderRef ! workflows.toList
      }
    case CreateWorkflow(wfDefName) =>
      router.route(CreateWorkflowExtended(wfDefName, idGenerator.nextId), sender())
  }
}

object ViewActor {
  def props(index: Int): Props = Props(new ViewActor(index))
}

class ViewActor(index: Int) extends Actor {
  val engineChild = context.actorOf(Props[EngineActor])
  var wfViews: Seq[WorkflowView] = List.empty

  def receive = {
    case GetWorkflows =>
      sender() ! wfViews
    case msg: CreateWorkflowExtended =>
      engineChild forward msg
    case msg: IdAllocatorActorRef =>
      engineChild ! msg
    case views: Seq[WorkflowView] @unchecked =>
      wfViews = views
  }
}

class EngineActor extends Actor {
  implicit var idGenerator: ActorBasedIdGenerator = _
  var engine: Engine = _
  val wfDefs = List(ExampleWorkflow, RandomWorkflow)

  def receive = {
    case CreateWorkflowExtended(wfDefName, id) =>
      idGenerator.forceNextId(id)
      wfDefs.find(_.name == wfDefName) match {
        case Some(wfDef) =>
          engine.startWorkflow(wfDef)
          sender() ! StartedWorkflow(wfDefName, id)
        case None =>
          sender() ! Error(s"Workflow definition $wfDefName not found.")
      }
    case IdAllocatorActorRef(ref) =>
      idGenerator = new ActorBasedIdGenerator(ref)
      engine = new Engine()
      context.system.scheduler.scheduleOnce(1 second, self, ExecuteRound)
    case ExecuteRound =>
      val updatedWfs = engine.executeRound
      //context.parent ! updatedWfs.map(wf => wf.view)
      context.system.scheduler.scheduleOnce(1 second, self, ExecuteRound)
  }
}

class ActorBasedIdGenerator(allocator: ActorRef) extends IdGenerator {
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout = Timeout(10 seconds)
  var ids = List.empty[Int]
  var allocation = _createAllocation()
  var forcedNextId: Option[Int] = None

  def _createAllocation(): Future[AllocatedIdBlock] = {
    val f = (allocator ? AllocateIdBlock).mapTo[AllocatedIdBlock]
    f onSuccess {
      case AllocatedIdBlock(block) =>
        ids ++= block
    }
    f
  }

  def _allocate(): Unit = {
    if (!allocation.isCompleted) {
      Await.ready(allocation, 10 seconds)
    } else {
      allocation = _createAllocation()
    }
  }

  override def nextId: Int = {
    if (ids.isEmpty) _allocate()
    val id = forcedNextId match {
      case None =>
        val _id = ids.head
        ids = ids.tail
        _id
      case Some(_id) =>
        forcedNextId = None
        _id
    }
    if (ids.length < 10) _allocate()
    id
  }

  def forceNextId(id: Int) = forcedNextId match {
    case None =>
      forcedNextId = Some(id)
    case Some(_) =>
      throw new IllegalStateException("Only one value can be forced for next id.")
  }
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


