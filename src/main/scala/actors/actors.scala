package actors

import akka.actor.Actor
import engine._

case class StartWorkflow(wfDef: WorkflowDefinition)
case class ExecuteRound()

class EngineActor extends Actor {
  val engine = new Engine()

  def receive = {
    case StartWorkflow(wfDef) =>
      engine.startWorkflow(wfDef)
      context.self ! ExecuteRound()
    case ExecuteRound() => engine.executeRound
  }
}
