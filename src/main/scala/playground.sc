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

val a1 = List(1, 2, 3)
val a2: Seq[Int] = List(4, 5, 6)

a1 zip a2

import spray.json._

class Color(val name: String, val red: Int, val green: Int, val blue: Int)
class Team(val name: String, val colors: Seq[Color])

object MyJsonProtocol extends DefaultJsonProtocol {
  implicit object ColorJsonFormat extends RootJsonFormat[Color] {
    def write(c: Color) =
      JsArray(JsString(c.name), JsNumber(c.red), JsNumber(c.green), JsNumber(c.blue))

    def read(value: JsValue) = value match {
      case JsArray(Vector(JsString(name), JsNumber(red), JsNumber(green), JsNumber(blue))) =>
        new Color(name, red.toInt, green.toInt, blue.toInt)
      case _ => serializationError("Color expected")
    }
  }

  implicit object TeamJsonFormat extends RootJsonFormat[Team] {
    def write(t: Team) = {
      JsObject("name" -> JsString(t.name), "color" -> t.colors.toJson)
    }

    def read(value: JsValue) = value match {
      case _ => serializationError("Color expected")
    }
  }
}

import MyJsonProtocol._

val json = new Color("CadetBlue", 95, 158, 160).toJson
val color = json.convertTo[Color]

new Team("team1", List(new Color("A", 1, 2, 3))).toJson
