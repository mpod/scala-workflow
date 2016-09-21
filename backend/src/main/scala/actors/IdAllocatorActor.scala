package actors

import actors.PrivateActorMessages.{AllocateIdBlock, AllocatedIdBlock}
import akka.actor.Actor
import common.PublicActorMessages.Error

class IdAllocatorActor extends Actor with akka.actor.ActorLogging {
  var lastId = 0
  val blockSize = 50

  def receive = {
    case AllocateIdBlock =>
      sender() ! AllocatedIdBlock(new Range(lastId, lastId + blockSize, 1).toList)
      lastId += blockSize
    case msg: Error =>
      log.error("Received error message: {}", msg.message)
    case _ =>
      sender() ! Error("Unknown message!")
  }
}

