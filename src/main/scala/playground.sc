case class A(a: String, b: String) {
  def c: String = a
}

val a = A("a", "b")

a.c

throw new UnknownError("aaaa")

