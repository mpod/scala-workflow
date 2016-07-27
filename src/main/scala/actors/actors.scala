package actors

import actors.WorkflowProtocol._
import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.routing.ConsistentHashingRouter.ConsistentHashMapping
import akka.routing._
import akka.util.Timeout
import engine._
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

object WorkflowProtocol {
  case object GetWorkflows
  case class CreateWorkflow(wfDef: WorkflowDefinition)
  case class IdAllocatorActorRef(ref: ActorRef)
  case class CreateWorkflowExtended(wfDef: WorkflowDefinition, id: Int)
  case object ExecuteRound
  case object AllocateIdBlock
  case class AllocatedIdBlock(identifiers: Seq[Int])
  case class WorkflowViews(wfViews: Seq[WorkflowView])
}

object WorkflowJsonProtocol extends DefaultJsonProtocol {
  implicit object TaskViewJsonFormat extends RootJsonFormat[TaskView] {
    override def write(tView: TaskView): JsValue =
      JsObject("name" -> JsString(tView.name), "id" -> JsNumber(tView.id), "state" -> JsString(tView.state.toString))

    override def read(json: JsValue): TaskView = deserializationError("Not implemented")
  }

  implicit object WorkflowViewJsonFormat extends RootJsonFormat[WorkflowView] {
    override def write(wfView: WorkflowView): JsValue =
      JsObject(
        "name" -> JsString(wfView.name),
        "id" -> JsNumber(wfView.id),
        "tasks" -> wfView.tasks.values.toList.toJson
      )

    override def read(json: JsValue): WorkflowView = deserializationError("Not implemented")
  }

  implicit val workflowViewsJsonFormat = jsonFormat1(WorkflowViews)
}

class TaskView(task: Task) {
  val id = task.id
  val state = task.state
  val name = task.taskDef.name
}

class WorkflowView(wf: Workflow) {
  val id = wf.id
  val name = wf.workflowDef.name
  val tasks = wf.tasks map (t => (t.id, new TaskView(t))) toMap
}

class RouterActor extends Actor {
  implicit val timeout = Timeout(10 seconds)
  var idGenerator: ActorBasedIdGenerator = _

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
      idGenerator = new ActorBasedIdGenerator(ref)
      context.become(initialized)
    case _ =>
      throw new RuntimeException("Not ready!")
  }

  def initialized: Receive = {
    case GetWorkflows =>
      val senderRef = sender()
      val f = Future.sequence(context.children map {c => (c ? GetWorkflows).mapTo[WorkflowViews]})
      f onSuccess {
        case workflows =>
          val wfExtract = for {
            wfs <- workflows
            wf <- wfs.wfViews
          } yield wf
          senderRef ! WorkflowViews(wfExtract toList)
      }
    case CreateWorkflow(wfDef) =>
      router.route(CreateWorkflowExtended(wfDef, idGenerator.nextId), sender())
  }
}

object ViewActor {
  def props(index: Int): Props = Props(new ViewActor(index))
}

class ViewActor(index: Int) extends Actor {
  val engineChild = context.actorOf(Props[EngineActor])
  var wfViews: Seq[WorkflowView] = List.empty[WorkflowView]

  def receive = {
    case GetWorkflows =>
      sender() ! WorkflowViews(wfViews)
    case msg: CreateWorkflowExtended =>
      engineChild forward msg
    case msg: IdAllocatorActorRef =>
      engineChild ! msg
    case WorkflowViews(views) =>
      wfViews = views
  }
}

class EngineActor extends Actor {
  implicit var idGenerator: ActorBasedIdGenerator = _
  var engine: Engine = _

  def receive = {
    case CreateWorkflowExtended(wfDef, id) =>
      idGenerator.forceNextId(id)
      engine.startWorkflow(wfDef)
      sender() ! s"Created workflow $id"
    case IdAllocatorActorRef(ref) =>
      idGenerator = new ActorBasedIdGenerator(ref)
      engine = new Engine()
      context.system.scheduler.scheduleOnce(1 second, self, ExecuteRound)
    case ExecuteRound =>
      val updatedWfs = engine.executeRound
      context.parent ! WorkflowViews(updatedWfs map (wf => new WorkflowView(wf)) toList)
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


