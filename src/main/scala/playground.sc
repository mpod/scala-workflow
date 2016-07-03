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

"a".mkString

class Color extends Enumeration {
  val Red = Value
  val Green = Value
  val Blue = Value
}

val clr = new Color

clr.Blue

class Service

new Service

Range(1, 5)

val d = Some(List(1,2,3))
val d2 = Some(List("a", "b"))

Option(List(1,2,3))

for {
  d1 <- d
  d3 <- d2
} yield d3

val e = Map(2->3, 4->5)

e.get(2)

Stream.from(1).head

"aaa".getClass

new java.lang.String("222")



