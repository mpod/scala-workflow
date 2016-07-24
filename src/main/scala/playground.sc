val a = List()

val b:Option[Int] = None

b.toString

trait Philosophical {
  def philosophize() {
    println("I consume memory, therefore I am!")
  }
}

class Frog extends Philosophical {

}

val c = new Frog()

def f(f1: Philosophical) {
  f1.philosophize()
}

f(c)

List("a", "b", "c").mkString(", ")

List(1, 2, 3)