package engine

import engine.ActionResult.Ok
import engine.Task.TaskContext

object ManualTaskDefinition {

  abstract class Field {
    type ValueType

    val label: String
    val name: String

    def value_=(value: ValueType)(implicit task: Task) = {
      task.put(name, value)
    }

    def value(implicit task: Task): Option[ValueType] = {
      if (task.contains(name))
        task.get[ValueType](name)
      else
        None
    }

    def isSet(implicit task: Task) = task.contains(name)

    def typeName: String
  }

  case class StringField(label: String, name: String) extends Field {
    type ValueType = String
    override def typeName = "String"
  }

  case class IntField(label: String, name: String) extends Field {
    type ValueType = Int
    override def typeName = "Int"
  }
}

class ManualTaskDefinition(val fields: List[ManualTaskDefinition.Field]) extends TaskDefinition {
  import ManualTaskDefinition._

  val fieldsMap: Map[String, Field] = fields.map(_.name).zip(fields).toMap

  override def action(implicit context: TaskContext): Option[ActionResult] = {
    if (allFieldsSet(context.task)) Some(Ok) else None
  }

  override def name: String = "Manual"

  def allFieldsSet(implicit task: Task) = fields.forall(_.isSet)

  def setField(name: String, value: String)(implicit task: Task) = fieldsMap.get(name) match {
    case Some(f: IntField) =>
      try {
        f.value_=(value.toInt)
      } catch {
        case e: java.lang.NumberFormatException =>
          throw new IllegalArgumentException("Field '%s' got a value '%s' but it expects a number.".format(name, value))
      }
    case Some(f: StringField) => f.value_=(value)
    case Some(f) =>
      throw new IllegalArgumentException(
        "Field %s is of type %s, while given value is of type %s".format(
          name, f.getClass.getName, value.getClass.getName
        )
      )
    case None =>
      throw new IllegalArgumentException("Field %s not found.".format(name))
  }
}
