package actors

import actors.PrivateActorMessages.{CreateWorkflowExtended, IdAllocatorActorRef}
import akka.actor.Actor
import akka.routing.{ActorRefRoutee, ConsistentHashingRoutingLogic, Router}
import akka.routing.ConsistentHashingRouter.ConsistentHashMapping
import akka.util.Timeout
import common.PublicActorMessages.{GetWorkflowDefinitions, GetWorkflows, StartWorkflow, Workflows}
import common.Views.WorkflowView

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.pattern.ask

import scala.concurrent.Future
import scala.util.Random

class RouterActor extends Actor {
  implicit val timeout = Timeout(10 seconds)
  var idGenerator: ActorBasedIdGenerator = _

  def hashMapping: ConsistentHashMapping = {
    case CreateWorkflowExtended(_, _, id) => id
    case GetWorkflowDefinitions => Random.nextInt()
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
        case workflows =>
          senderRef ! Workflows(workflows.toList.flatten)
      }
    case StartWorkflow(wfDefName, label) =>
      router.route(CreateWorkflowExtended(wfDefName, label, idGenerator.nextId), sender())
    case GetWorkflowDefinitions =>
      router.route(GetWorkflowDefinitions, sender())
  }
}

