package actors

import actors.PrivateActorMessages.{AllocateIdBlock, AllocatedIdBlock}
import akka.actor.ActorRef
import akka.util.Timeout
import engine.IdGenerator

import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.concurrent.duration._
import akka.pattern.ask

class ActorBasedIdGenerator(allocator: ActorRef) extends IdGenerator {
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout = Timeout(10 seconds)
  var ids = List.empty[Int]
  var allocation = _createAllocation()
  var forcedNextId: Option[Int] = None

  def _createAllocation(): Future[AllocatedIdBlock] = {
    val f = (allocator ? AllocateIdBlock).mapTo[AllocatedIdBlock]
    f onSuccess {
      case AllocatedIdBlock(block) =>
        ids ++= block
    }
    f
  }

  def _allocate(): Unit = {
    if (!allocation.isCompleted) {
      Await.ready(allocation, 10 seconds)
    } else {
      allocation = _createAllocation()
    }
  }

  override def nextId: Int = {
    if (ids.isEmpty) _allocate()
    val id = forcedNextId match {
      case None =>
        val _id = ids.head
        ids = ids.tail
        _id
      case Some(_id) =>
        forcedNextId = None
        _id
    }
    if (ids.length < 10) _allocate()
    id
  }

  def forceNextId(id: Int) = forcedNextId match {
    case None =>
      forcedNextId = Some(id)
    case Some(_) =>
      throw new IllegalStateException("Only one value can be forced for next id.")
  }
}

