package actors

import actors.PrivateActorMessages.{CreateWorkflowExtended, IdAllocatorActorRef}
import akka.actor.{Actor, Props}
import common.PublicActorMessages.{GetWorkflowDefinitions, GetWorkflows, Workflows}
import common.Views.WorkflowView

object ViewActor {
  def props(index: Int): Props = Props(new ViewActor(index))
}

class ViewActor(index: Int) extends Actor {
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
  }
}

