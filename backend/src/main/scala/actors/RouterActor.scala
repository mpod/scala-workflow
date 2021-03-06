package actors

import actors.PrivateActorMessages.{CreateWorkflowExtended, IdAllocatorActorRef}
import akka.actor.Actor
import akka.routing.{ActorRefRoutee, ConsistentHashingRoutingLogic, Router}
import akka.routing.ConsistentHashingRouter.ConsistentHashMapping
import akka.util.Timeout
import common.PublicActorMessages._
import common.Views.WorkflowView

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.pattern.ask

import scala.concurrent.Future
import scala.util.Random

class RouterActor extends Actor with akka.actor.ActorLogging {
  implicit val timeout = Timeout(10 seconds)
  var idGenerator: ActorBasedIdGenerator = _

  def hashMapping: ConsistentHashMapping = {
    case CreateWorkflowExtended(_, _, wfId) => wfId
    case GetWorkflowDefinitions => Random.nextInt()
    case ExecuteManualTask(wfRootId, _, _, _) => wfRootId
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
      sender() ! Error("Router not ready!")
  }

  def initialized: Receive = {
    case GetWorkflows =>
      val senderRef = sender()
      val f = Future.sequence(context.children map {c => (c ? GetWorkflows).mapTo[Seq[WorkflowView]]})
      f onSuccess {
        case workflows =>
          senderRef ! Workflows(workflows.toList.flatten)
      }
    case StartWorkflow(wfDefName, label) =>
      router.route(CreateWorkflowExtended(wfDefName, label, idGenerator.nextId), sender())
    case GetWorkflowDefinitions =>
      router.route(GetWorkflowDefinitions, sender())
    case m: ExecuteManualTask =>
      router.route(m, sender())
    case msg: Error =>
      log.error("Received error message: {}", msg.message)
    case _ =>
      sender() ! Error("Unknown message!")
  }
}

