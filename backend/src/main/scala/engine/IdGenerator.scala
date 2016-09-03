package engine

abstract class IdGenerator {
  def nextId: Int
}

object IdGenerator {

  object SimpleIdGenerator extends IdGenerator {
    var id = 0

    override def nextId: Int = {
      id += 1
      id
    }
  }

}
