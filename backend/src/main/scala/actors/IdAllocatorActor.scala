package actors

import actors.PrivateActorMessages.{AllocateIdBlock, AllocatedIdBlock}
import akka.actor.Actor

class IdAllocatorActor extends Actor {
  var lastId = 0
  val blockSize = 50

  def receive = {
    case AllocateIdBlock =>
      sender() ! AllocatedIdBlock(new Range(lastId, lastId + blockSize, 1).toList)
      lastId += blockSize
  }
}

