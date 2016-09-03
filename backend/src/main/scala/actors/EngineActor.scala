package actors

import actors.PrivateActorMessages.{CreateWorkflowExtended, ExecuteRound, IdAllocatorActorRef}
import akka.actor.Actor
import common.PublicActorMessages.{Error, Workflows}
import engine.Engine
import scala.concurrent.duration._
import scala.language.postfixOps
import engine.ImplicitConversions._


class EngineActor extends Actor {
  implicit var idGenerator: ActorBasedIdGenerator = _
  var engine: Engine = _

  def receive = {
    case CreateWorkflowExtended(wfDefName, label, id) =>
      idGenerator.forceNextId(id)
      engine.workflowDefinitions.find(_.name == wfDefName) match {
        case Some(wfDef) =>
          val wf = engine.startWorkflow(wfDef, label)
          sender() ! Workflows(List(wf))
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

