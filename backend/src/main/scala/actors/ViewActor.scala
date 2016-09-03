package actors

import actors.PrivateActorMessages.{CreateWorkflowExtended, IdAllocatorActorRef}
import akka.actor.{Actor, Props}
import common.PublicActorMessages.GetWorkflows
import common.Views.WorkflowView

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

