package actors

import akka.actor.Actor
import engine._

case class StartWorkflow(wfDef: WorkflowDefinition)
case class ExecuteRound()
case class AllWorkflowsFinished()
case class SomeWorkflowsUpdated()

class EngineActor extends Actor {
  val engine = new Engine()

  def receive = {
    case StartWorkflow(wfDef) =>
      engine.startWorkflow(wfDef)
      sender ! "Workflow Started"
    case ExecuteRound() =>
      if (engine.executeRound.nonEmpty)
        sender ! SomeWorkflowsUpdated()
      else
        sender ! AllWorkflowsFinished()
  }
}
