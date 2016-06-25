import engine.Task

object Workflow {
  def main(args: Array[String]): Unit = {
    val a = Task("a")
    val b = Task("b")
    val c = Task("c")

    a.addChild(b)
    a.addChild(c)
    println(a)
  }
}
