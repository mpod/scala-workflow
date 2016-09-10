package actors

import actors.PrivateActorMessages.{CreateWorkflowExtended, ExecuteRound, IdAllocatorActorRef}
import akka.actor.Actor
import common.PublicActorMessages._
import engine.Engine

import scala.concurrent.duration._
import scala.language.postfixOps
import engine.ImplicitConversions._

import scala.concurrent.ExecutionContext.Implicits.global

class EngineActor extends Actor {
  implicit var idGenerator: ActorBasedIdGenerator = _
  var engine: Engine = _

  def receive = uninitialized

  def uninitialized: Receive = {
    case IdAllocatorActorRef(ref) =>
      idGenerator = new ActorBasedIdGenerator(ref)
      engine = new Engine()
      context.become(initialized)
      context.system.scheduler.scheduleOnce(1 second, self, ExecuteRound)
    case _ =>
      sender() ! Error("Engine actor not ready!")
  }

  def initialized: Receive = {
    case GetWorkflowDefinitions =>
      sender() ! WorkflowDefinitions(engine.workflowDefinitions map (_.name))
    case CreateWorkflowExtended(wfDefName, label, id) =>
      idGenerator.forceNextId(id)
      engine.workflowDefinitions.find(_.name == wfDefName) match {
        case Some(wfDef) =>
          val wf = engine.startWorkflow(wfDef, label)
          context.parent ! Workflows(engine.workflows)
          sender() ! Workflows(List(wf))
        case None =>
          sender() ! Error(s"Workflow definition $wfDefName not found.")
      }
    case ExecuteRound =>
      engine.executeRound
      context.parent ! Workflows(engine.workflows)
      context.system.scheduler.scheduleOnce(1 second, self, ExecuteRound)
    case ExecuteManualTask(wfId, taskId, fieldValues) =>
      engine.setManualTaskFields(wfId, taskId, fieldValues)
      context.parent ! Workflows(engine.workflows)
      sender() ! Workflows(engine.workflows)
    case msg: Error =>
      /* TODO: Log error message */
    case _ =>
      sender() ! Error("Unknown message!")
  }
}

