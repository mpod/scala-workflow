package actors

import actors.PrivateActorMessages.{CreateWorkflowExtended, IdAllocatorActorRef}
import akka.actor.{Actor, Props}
import common.PublicActorMessages._
import common.Views.WorkflowView

object ViewActor {
  def props(index: Int): Props = Props(new ViewActor(index))
}

class ViewActor(index: Int) extends Actor with akka.actor.ActorLogging {
  val engineChild = context.actorOf(Props[EngineActor])
  var wfViews: Seq[WorkflowView] = List.empty

  def receive = {
    case GetWorkflowDefinitions =>
      engineChild forward GetWorkflowDefinitions
    case GetWorkflows =>
      sender() ! wfViews
    case msg: CreateWorkflowExtended =>
      engineChild forward msg
    case msg: IdAllocatorActorRef =>
      engineChild ! msg
    case Workflows(views) =>
      wfViews = views
    case msg: ExecuteManualTask =>
      engineChild forward msg
    case msg: Error =>
      log.error("Received error message: {}", msg.message)
    case _ =>
      sender() ! Error("Unknown message!")
  }
}

