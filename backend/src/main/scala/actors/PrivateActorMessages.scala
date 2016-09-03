package actors

import akka.actor.ActorRef

object PrivateActorMessages {
  case class IdAllocatorActorRef(ref: ActorRef)
  case class CreateWorkflowExtended(wfDefName: String, label: String, id: Int)
  case object ExecuteRound
  case object AllocateIdBlock
  case class AllocatedIdBlock(identifiers: Seq[Int])
}

